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

    /** ✅ Mua thẻ & hoàn tất giao dịch ngay */
    @Transactional
    @Override
    public TransactionResponse createAndCompleteTransaction(TransactionRequest request, Long buyerUserId) {
        Inventory inventory = inventoryRepository.findByInventoryIdAndIsForSaleTrue(request.getInventoryId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_CARD_NOT_FOUND));

        User buyer = userRepository.findById(buyerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        User seller = userRepository.findById(inventory.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (buyer.getUserId().equals(seller.getUserId())) {
            throw new AppException(ErrorCode.INVALID_TRANSACTION);
        }

        if (buyer.getBalance().compareTo(inventory.getSalePrice()) < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // ✅ Trừ tiền buyer, cộng tiền seller
        buyer.setBalance(buyer.getBalance().subtract(inventory.getSalePrice()));
        seller.setBalance(seller.getBalance().add(inventory.getSalePrice()));
        userRepository.save(buyer);
        userRepository.save(seller);

        // ✅ Tạo transaction & set trạng thái COMPLETED luôn
        Transaction transaction = new Transaction();
        transaction.setSellerId(seller.getUserId());
        transaction.setBuyerId(buyer.getUserId());
        transaction.setInventoryId(inventory.getInventoryId());
        transaction.setPrice(inventory.getSalePrice());
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        // ✅ Chuyển quyền sở hữu thẻ
        inventory.setUserId(buyer.getUserId());
        inventory.setIsForSale(false);
        inventory.setIsOnDeck(false);
        inventoryRepository.save(inventory);

        // ✅ Log & Kafka
        saveAuditLogAsync(AuditAction.COMPLETE_TRANSACTION, buyer.getUsername(), buyer.getUserId(),
                inventory.getCardId(),
                "Hoàn tất giao dịch auto: chuyển thẻ " + inventory.getCardId() + " từ user " +
                        seller.getUserId() + " sang " + buyer.getUserId());

        sendTransactionEventAsync(AuditAction.COMPLETE_TRANSACTION, transaction, transaction.getPrice());

        // ✅ Cache
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

    /** ✅ Cancel chỉ cho phép nếu giao dịch vẫn ở trạng thái PENDING */
    @Transactional
    @Override
    public TransactionResponse cancelTransaction(Long transactionId, Long userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_TRANSACTION_STATUS);
        }

        if (!transaction.getBuyerId().equals(userId) && !transaction.getSellerId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        transaction.setStatus(TransactionStatus.CANCELLED);
        transaction.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        saveAuditLogAsync(AuditAction.CANCEL_TRANSACTION, userId.toString(), transaction.getBuyerId(),
                null, "Hủy giao dịch " + transactionId + " bởi user " + userId);

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
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setActorUsername(actorUsername);
        log.setTargetUserId(targetUserId);
        log.setCardId(cardId);
        log.setDescription(description);
        log.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(log);
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