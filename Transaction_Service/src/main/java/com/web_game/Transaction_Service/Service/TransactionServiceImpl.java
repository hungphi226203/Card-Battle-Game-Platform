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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    @Transactional
    @Override
    public TransactionResponse createAndCompleteTransaction(TransactionRequest request, Long buyerUserId) {
        // 1. Lock inventory để tránh race condition
        Inventory inventory = inventoryRepository.lockByIdForUpdate(request.getInventoryId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_CARD_NOT_FOUND));

        // 2. Kiểm tra thẻ có còn được bán không
        if (!Boolean.TRUE.equals(inventory.getIsForSale())) {
            throw new AppException(ErrorCode.CARD_ALREADY_SOLD);
        }

        // 3. Lấy thông tin user và kiểm tra
        User buyer = userRepository.findById(buyerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        User seller = userRepository.findById(inventory.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 4. Kiểm tra buyer không thể mua thẻ của chính mình
        if (buyerUserId.equals(inventory.getUserId())) {
            throw new AppException(ErrorCode.CANNOT_BUY_OWN_CARD);
        }

        // 5. Kiểm tra số dư
        Float buyerBalance = Optional.ofNullable(buyer.getBalance()).orElse(0f);
        Float sellerBalance = Optional.ofNullable(seller.getBalance()).orElse(0f);

        if (buyerBalance.compareTo(inventory.getSalePrice()) < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // 6. Lưu giá bán trước khi cập nhật inventory
        Float salePrice = inventory.getSalePrice();

        // 7. Cập nhật số dư
        buyer.setBalance(buyerBalance - salePrice);
        seller.setBalance(sellerBalance + salePrice);

        // 8. Cập nhật ownership của thẻ - QUAN TRỌNG!
        inventory.setUserId(buyerUserId);
        inventory.setIsForSale(false);
        inventory.setSalePrice(null);
        inventory.setAcquiredAt(LocalDateTime.now());

        // 9. Lưu tất cả thay đổi
        userRepository.save(buyer);
        userRepository.save(seller);
        inventoryRepository.save(inventory);

        // 10. Tạo transaction record
        Transaction transaction = new Transaction();
        transaction.setSellerId(seller.getUserId());
        transaction.setBuyerId(buyer.getUserId());
        transaction.setInventoryId(inventory.getInventoryId());
        transaction.setPrice(salePrice); // Sử dụng giá đã lưu trước đó
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        Transaction savedTransaction = transactionRepository.save(transaction);

        // 11. Gửi Kafka event
        sendTransactionEventAsync(AuditAction.COMPLETE_TRANSACTION, savedTransaction, salePrice);

        // 12. Log audit
        saveAuditLogAsync(AuditAction.COMPLETE_TRANSACTION, buyer.getUsername(), buyer.getUserId(),
                inventory.getCardId(), "Giao dịch hoàn tất: user " + buyer.getUserId() +
                        " mua thẻ từ user " + seller.getUserId());

        return toResponse(savedTransaction);
    }

    @Override
    public List<TransactionResponse> getUserTransactions(Long userId, String role) {
        List<Transaction> transactions;

        if ("buyer".equalsIgnoreCase(role)) {
            transactions = transactionRepository.findByBuyerId(userId);
        } else if ("seller".equalsIgnoreCase(role)) {
            transactions = transactionRepository.findBySellerId(userId);
        } else {
            transactions = transactionRepository.findByBuyerIdOrSellerId(userId, userId);
        }

        return transactions.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public TransactionResponse getTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));
        return toResponse(transaction);
    }

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

        // Nếu cancel, cần restore lại inventory status
        try {
            Inventory inventory = inventoryRepository.findById(transaction.getInventoryId()).orElse(null);
            if (inventory != null && inventory.getUserId().equals(transaction.getSellerId())) {
                inventory.setIsForSale(true);
                inventory.setSalePrice(transaction.getPrice());
                inventoryRepository.save(inventory);
            }
        } catch (Exception ignored) {}

        try {
            saveAuditLogAsync(AuditAction.CANCEL_TRANSACTION, userId.toString(), transaction.getBuyerId(),
                    null, "Hủy giao dịch " + transactionId + " bởi user " + userId);
            sendTransactionEventAsync(AuditAction.CANCEL_TRANSACTION, transaction, transaction.getPrice());
        } catch (Exception ignored) {}

        return toResponse(transaction);
    }

    private TransactionResponse toResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        BeanUtils.copyProperties(transaction, response);
        response.setStatus(transaction.getStatus().name());
        return response;
    }

    @Async
    public void saveAuditLogAsync(String action, String actorUsername, Long targetUserId, Long cardId, String description) {
        try {
            AuditLog log = new AuditLog();
            log.setAction(action);
            log.setActorUsername(actorUsername);
            log.setTargetUserId(targetUserId);
            log.setCardId(cardId);
            log.setDescription(description);
            log.setTimestamp(LocalDateTime.now());
            auditLogRepository.save(log);
        } catch (Exception ignored) {}
    }

    @Async
    public void sendTransactionEventAsync(String action, Transaction transaction, Float price) {
        try {
            TransactionEvent event = new TransactionEvent();
            event.setAction(action);
            event.setTransactionId(transaction.getTransactionId());
            event.setSellerId(transaction.getSellerId());
            event.setBuyerId(transaction.getBuyerId());
            event.setInventoryId(transaction.getInventoryId());
            event.setPrice(price);
            event.setTimestamp(LocalDateTime.now());
            kafkaTemplate.send("transaction-events", event);
        } catch (Exception ignored) {}
    }
}