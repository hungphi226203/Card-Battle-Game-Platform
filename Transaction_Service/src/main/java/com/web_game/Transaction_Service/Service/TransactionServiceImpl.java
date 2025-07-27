package com.web_game.Transaction_Service.Service;

import com.web_game.Transaction_Service.Repository.AuditLogRepository;
import com.web_game.Transaction_Service.Repository.InventoryRepository;
import com.web_game.Transaction_Service.Repository.TransactionRepository;
import com.web_game.Transaction_Service.Repository.UserRepository;
import com.web_game.common.Constant.AuditAction;
import com.web_game.common.DTO.Request.Transaction.TransactionRequest;
import com.web_game.common.DTO.Respone.TransactionResponse;
import com.web_game.common.Entity.AuditLog;
import com.web_game.common.Entity.Inventory;
import com.web_game.common.Entity.Transaction;
import com.web_game.common.Entity.User;
import com.web_game.common.Enum.TransactionStatus;
import com.web_game.common.Event.TransactionEvent;
import com.web_game.common.Exception.AppException;
import com.web_game.common.Exception.ErrorCode;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TRANSACTION_CACHE_KEY = "transaction:";

    @Transactional
    @Override
    public TransactionResponse createTransaction(TransactionRequest request, Long buyerUserId) {
        Inventory inventory = inventoryRepository.findByInventoryIdAndIsForSaleTrue(request.getInventoryId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_CARD_NOT_FOUND));

        User buyer = userRepository.findById(buyerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (buyer.getBalance().compareTo(inventory.getSalePrice()) < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        User seller = userRepository.findById(inventory.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (seller.getUserId().equals(buyer.getUserId())) {
            throw new AppException(ErrorCode.INVALID_TRANSACTION);
        }

        Transaction transaction = new Transaction();
        transaction.setSellerId(seller.getUserId());
        transaction.setBuyerId(buyer.getUserId());
        transaction.setInventoryId(inventory.getInventoryId());
        transaction.setPrice(inventory.getSalePrice());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        // Log + Kafka
        saveAuditLogAsync(AuditAction.CREATE_TRANSACTION, buyer.getUsername(), seller.getUserId(), inventory.getCardId(),
                "Tạo giao dịch mua thẻ " + inventory.getCardId() + " từ user " + seller.getUserId());

        sendTransactionEventAsync(AuditAction.CREATE_TRANSACTION, transaction, transaction.getPrice());

        // Cache
        TransactionResponse response = toResponse(transaction);
        redisTemplate.opsForValue().set(TRANSACTION_CACHE_KEY + transaction.getTransactionId(), response, 1, TimeUnit.HOURS);
        return response;
    }

    @Override
    public TransactionResponse getTransaction(Long transactionId) {
        String key = TRANSACTION_CACHE_KEY + transactionId;
        TransactionResponse cached = (TransactionResponse) redisTemplate.opsForValue().get(key);
        if (cached != null) return cached;

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));
        TransactionResponse response = toResponse(transaction);

        redisTemplate.opsForValue().set(key, response, 1, TimeUnit.HOURS);
        return response;
    }

    @Transactional
    @Override
    public TransactionResponse completeTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        if (transaction.getStatus() != TransactionStatus.PENDING)
            throw new AppException(ErrorCode.INVALID_TRANSACTION_STATUS);

        Inventory inventory = inventoryRepository.findById(transaction.getInventoryId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_CARD_NOT_FOUND));

        User buyer = userRepository.findById(transaction.getBuyerId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        User seller = userRepository.findById(transaction.getSellerId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (buyer.getBalance().compareTo(transaction.getPrice()) < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // Cập nhật tiền
        buyer.setBalance(buyer.getBalance().subtract(transaction.getPrice()));
        seller.setBalance(seller.getBalance().add(transaction.getPrice()));
        userRepository.save(buyer);
        userRepository.save(seller);

        // Cập nhật trạng thái
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        // Chuyển quyền sở hữu inventory
        inventory.setUserId(buyer.getUserId());
        inventory.setIsForSale(false);
        inventory.setIsOnDeck(false);
        inventoryRepository.save(inventory);

        saveAuditLogAsync(AuditAction.COMPLETE_TRANSACTION, "SYSTEM", buyer.getUserId(), inventory.getCardId(),
                "Hoàn tất giao dịch " + transactionId + ": chuyển thẻ từ user " + seller.getUserId() + " sang " + buyer.getUserId());

        sendTransactionEventAsync(AuditAction.COMPLETE_TRANSACTION, transaction, transaction.getPrice());

        TransactionResponse response = toResponse(transaction);
        redisTemplate.opsForValue().set(TRANSACTION_CACHE_KEY + transactionId, response, 1, TimeUnit.HOURS);
        return response;
    }

    @Transactional
    @Override
    public TransactionResponse cancelTransaction(Long transactionId, Long userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        if (transaction.getStatus() != TransactionStatus.PENDING)
            throw new AppException(ErrorCode.INVALID_TRANSACTION_STATUS);

        if (!transaction.getBuyerId().equals(userId) && !transaction.getSellerId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        transaction.setStatus(TransactionStatus.CANCELLED);
        transaction.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        Inventory inventory = inventoryRepository.findById(transaction.getInventoryId())
                .orElse(null);
        Long cardId = inventory != null ? inventory.getCardId() : null;

        saveAuditLogAsync(AuditAction.CANCEL_TRANSACTION, userId.toString(), transaction.getBuyerId(), cardId,
                "Hủy giao dịch " + transactionId + " bởi userId " + userId);

        sendTransactionEventAsync(AuditAction.CANCEL_TRANSACTION, transaction, transaction.getPrice());

        TransactionResponse response = toResponse(transaction);
        redisTemplate.opsForValue().set(TRANSACTION_CACHE_KEY + transactionId, response, 1, TimeUnit.HOURS);
        return response;
    }

    private TransactionResponse toResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        BeanUtils.copyProperties(transaction, response);
        response.setStatus(transaction.getStatus().name());
        return response;
    }

    @Async
    public void saveAuditLogAsync(String action, String actorUsername, Long targetUserId, Long cardId, String description) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setActorUsername(actorUsername);
        auditLog.setTargetUserId(targetUserId);
        auditLog.setCardId(cardId);
        auditLog.setDescription(description);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(auditLog);
    }

    @Async
    public void sendTransactionEventAsync(String action, Transaction transaction, BigDecimal price) {
        TransactionEvent event = new TransactionEvent();
        event.setAction(action);
        event.setTransactionId(transaction.getTransactionId());
        event.setSellerId(transaction.getSellerId());
        event.setBuyerId(transaction.getBuyerId());
        event.setInventoryId(transaction.getInventoryId());
        event.setPrice(price);
        event.setTimestamp(LocalDateTime.now());
        kafkaTemplate.send("transaction-events", event);
    }
}