package com.web_game.Inventory_Service.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web_game.Inventory_Service.Repository.CardRepository;
import com.web_game.Inventory_Service.Repository.AuditLogRepository;
import com.web_game.Inventory_Service.Repository.UserCardRepository;
import com.web_game.Inventory_Service.Repository.UserRepository;
import com.web_game.common.Constant.AuditAction;
import com.web_game.common.DTO.Request.UserCard.SellCardRequest;
import com.web_game.common.DTO.Respone.InventoryResponse;
import com.web_game.common.DTO.shared.UserCardDTO;
import com.web_game.common.Entity.AuditLog;
import com.web_game.common.Entity.Card;
import com.web_game.common.Entity.Inventory;
import com.web_game.common.Enum.Rarity;
import com.web_game.common.Event.GachaEvent;
import com.web_game.common.Event.InventoryEvent;
import com.web_game.common.Event.NotificationMessage;
import com.web_game.common.Event.TransactionEvent;
import com.web_game.common.Event.UserRegisteredEvent;
import com.web_game.common.Exception.AppException;
import com.web_game.common.Exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final UserCardRepository userCardRepository;
    private final CardRepository cardRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    private final KafkaTemplate<String, InventoryEvent> inventoryKafkaTemplate;

    @Autowired
    private KafkaTemplate<String, NotificationMessage> notificationKafkaTemplate;

    private final ObjectMapper objectMapper;

    public List<UserCardDTO> getInventory(Long userId) {
        return userCardRepository.findByUserId(userId).stream()
                .map(inv -> {
                    Card card = inv.getCard();
                    return new UserCardDTO(
                            inv.getInventoryId(),
                            card != null ? card.getCardId() : null,
                            card != null ? card.getRarity() : null,
                            card != null ? card.getName() : null,
                            card != null ? card.getImageUrl() : null,
                            card != null ? card.getMainImg() : null,
                            card != null ? card.getMana() : null,
                            card != null ? card.getAttack() : null,
                            card != null ? card.getHealth() : null,
                            Boolean.TRUE.equals(inv.getIsOnDeck()),
                            Boolean.TRUE.equals(inv.getIsForSale()),
                            inv.getSalePrice(),
                            inv.getAcquiredAt()
                    );
                })
                .collect(Collectors.toList());
    }

    public UserCardDTO getCardInInventory(Long userId, Long cardId) {
        Inventory inventory = userCardRepository.findByUserUserIdAndCardCardId(userId, cardId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_CARD_NOT_FOUND));
        UserCardDTO dto = new UserCardDTO();
        BeanUtils.copyProperties(inventory, dto);
        return dto;
    }

    @Transactional
    public void addCardToInventory(Long userId, Long cardId, String actorUsername) {
        // Get card details for notification
        Optional<Card> cardOpt = cardRepository.findById(cardId);
        if (!cardOpt.isPresent()) {
            log.error("Card not found: {}", cardId);
            throw new AppException(ErrorCode.INVALID_CARD_VALUE);
        }

        Card card = cardOpt.get();

        Inventory inventory = new Inventory();
        inventory.setUserId(userId);
        inventory.setCardId(cardId);
        inventory.setAcquiredAt(LocalDateTime.now());
        inventory.setIsForSale(false);
        inventory.setIsOnDeck(false);

        Inventory savedInventory = userCardRepository.save(inventory);

        log.info("Added card {} to user {} inventory", cardId, userId);

        // Save audit log
        saveAuditLogAsync(
                actorUsername.equals("SYSTEM") ? AuditAction.GACHA_CARD : AuditAction.ADD_CARD,
                actorUsername,
                userId,
                cardId,
                actorUsername.equals("SYSTEM") ?
                        "Thêm thẻ " + cardId + " vào kho của user " + userId + " qua gacha" :
                        "Thêm thẻ " + cardId + " vào kho của user " + userId + " bởi " + actorUsername
        );

        // Send inventory event
        sendInventoryEventAsync(
                actorUsername.equals("SYSTEM") ? AuditAction.GACHA_CARD : AuditAction.ADD_CARD,
                userId,
                cardId,
                savedInventory.getInventoryId(),
                null
        );

        // 🚀 Send WebSocket notification to Unity
        sendInventoryNotification(userId, cardId, card, "CARD_ADDED", savedInventory.getInventoryId());
    }

    @Transactional
    public void removeCardFromInventory(Long userId, Long cardId, String actorUsername) {
        Inventory inventory = userCardRepository.findByUserUserIdAndCardCardId(userId, cardId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_CARD_NOT_FOUND));

        // Get card details for notification
        Optional<Card> cardOpt = cardRepository.findById(cardId);

        userCardRepository.delete(inventory);

        saveAuditLogAsync(AuditAction.REMOVE_CARD, actorUsername, userId, cardId,
                "Xóa thẻ " + cardId + " khỏi kho của user " + userId + " bởi " + actorUsername);

        sendInventoryEventAsync(AuditAction.REMOVE_CARD, userId, cardId, inventory.getInventoryId(), null);

        // Send notification for card removal
        if (cardOpt.isPresent()) {
            sendInventoryNotification(userId, cardId, cardOpt.get(), "CARD_REMOVED", inventory.getInventoryId());
        }
    }

    @Transactional
    public void listCardForSale(Long inventoryId, SellCardRequest request, Long userId) {
        Inventory inventory = userCardRepository.findByInventoryIdAndUserId(inventoryId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_CARD_NOT_FOUND));

        if (Boolean.TRUE.equals(inventory.getIsForSale())) {
            throw new AppException(ErrorCode.CARD_ALREADY_FOR_SALE);
        }
        if (Boolean.TRUE.equals(inventory.getIsOnDeck())) {
            throw new AppException(ErrorCode.CARD_IN_DECK_CANNOT_BE_SOLD);
        }

        inventory.setIsForSale(true);
        inventory.setSalePrice(request.getSalePrice());
        userCardRepository.save(inventory);

        saveAuditLogAsync(AuditAction.LIST_CARD_FOR_SALE, "USER", userId, inventory.getCardId(),
                "Rao bán thẻ " + inventory.getCardId() + " với giá " + request.getSalePrice());

        sendInventoryEventAsync(AuditAction.LIST_CARD_FOR_SALE, userId, inventory.getCardId(), inventoryId, request.getSalePrice());

        // Send notification for card listed for sale
        Optional<Card> cardOpt = cardRepository.findById(inventory.getCardId());
        if (cardOpt.isPresent()) {
            sendCardSaleNotification(userId, inventory.getCardId(), cardOpt.get(), "CARD_LISTED_FOR_SALE", request.getSalePrice());
        }
    }

    @Transactional
    public void cancelCardSale(Long inventoryId, Long userId) {
        Inventory inventory = userCardRepository.findByInventoryIdAndUserId(inventoryId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_CARD_NOT_FOUND));

        if (!Boolean.TRUE.equals(inventory.getIsForSale())) {
            throw new AppException(ErrorCode.CARD_NOT_FOR_SALE);
        }

        inventory.setIsForSale(false);
        inventory.setSalePrice(null);
        userCardRepository.save(inventory);

        saveAuditLogAsync(AuditAction.CANCEL_CARD_SALE, "USER", userId, inventory.getCardId(),
                "Hủy rao bán thẻ " + inventory.getCardId());

        sendInventoryEventAsync(AuditAction.CANCEL_CARD_SALE, userId, inventory.getCardId(), inventoryId, null);

        // Send notification for sale cancellation
        Optional<Card> cardOpt = cardRepository.findById(inventory.getCardId());
        if (cardOpt.isPresent()) {
            sendCardSaleNotification(userId, inventory.getCardId(), cardOpt.get(), "CARD_SALE_CANCELLED", null);
        }
    }

    // Existing methods remain the same...
    public List<InventoryResponse> getCardsForSale() {
        return userCardRepository.findByIsForSaleTrueAndIsOnDeckFalse().stream()
                .map(inventory -> {
                    InventoryResponse response = new InventoryResponse();
                    BeanUtils.copyProperties(inventory, response);

                    cardRepository.findById(inventory.getCardId()).ifPresent(card -> {
                        response.setCardName(card.getName());
                        response.setImageUrl(card.getImageUrl());
                        response.setMainImg(card.getMainImg());
                        response.setRarity(card.getRarity());
                        response.setType(card.getType());
                    });

                    userRepository.findById(inventory.getUserId()).ifPresent(user -> {
                        response.setSellerName(user.getFullName());
                    });

                    return response;
                })
                .collect(Collectors.toList());
    }

    public List<InventoryResponse> getMyCardsForSale(Long userId) {
        return userCardRepository.findByUserIdAndIsForSaleTrueAndIsOnDeckFalse(userId)
                .stream()
                .map(inventory -> {
                    InventoryResponse response = new InventoryResponse();
                    BeanUtils.copyProperties(inventory, response);

                    cardRepository.findById(inventory.getCardId()).ifPresent(card -> {
                        response.setCardName(card.getName());
                        response.setImageUrl(card.getImageUrl());
                        response.setMainImg(card.getMainImg());
                        response.setRarity(card.getRarity());
                        response.setType(card.getType());
                    });

                    userRepository.findById(userId).ifPresent(user -> {
                        response.setSellerName(user.getFullName());
                    });

                    return response;
                })
                .collect(Collectors.toList());
    }

    @KafkaListener(topics = "gacha-events", groupId = "inventory-group", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void handleGachaEvent(GachaEvent event) {
        log.info("Processing gacha event for user: {}, card: {}", event.getUserId(), event.getCardId());
        try {
            addCardToInventory(event.getUserId(), event.getCardId(), "SYSTEM");
            log.info("Successfully processed gacha event for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to process gacha event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = "transaction-events", groupId = "inventory-group", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void handleTransactionEvent(TransactionEvent event) {
        log.info("Processing transaction event: {}", event.getAction());

        if (AuditAction.COMPLETE_TRANSACTION.equals(event.getAction())) {
            Inventory inventory = userCardRepository.findById(event.getInventoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_CARD_NOT_FOUND));

            Long previousOwnerId = inventory.getUserId();
            inventory.setUserId(event.getBuyerId());
            inventory.setIsForSale(false);
            inventory.setIsOnDeck(false);
            inventory.setAcquiredAt(LocalDateTime.now());
            userCardRepository.save(inventory);

            saveAuditLogAsync(AuditAction.COMPLETE_TRANSACTION, "SYSTEM", event.getBuyerId(),
                    inventory.getCardId(), "Ownership updated via Kafka for inventory " + inventory.getInventoryId());

            sendInventoryEventAsync(AuditAction.TRANSFER_CARD, event.getBuyerId(), inventory.getCardId(),
                    inventory.getInventoryId(), null);

            // 🚀 Send notifications for card transfer
            Optional<Card> cardOpt = cardRepository.findById(inventory.getCardId());
            if (cardOpt.isPresent()) {
                Card card = cardOpt.get();

                // Notify buyer - they got a new card
                sendInventoryNotification(event.getBuyerId(), inventory.getCardId(), card, "CARD_PURCHASED", inventory.getInventoryId());

                // Notify seller - their card was sold
                sendCardSaleNotification(previousOwnerId, inventory.getCardId(), card, "CARD_SOLD", event.getPrice());
            }
        }
    }

    @KafkaListener(topics = "user-registered-topic", groupId = "inventory-group", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Setting up starter inventory for new user: {}", event.getUserId());

        Long userId = event.getUserId();
        List<Card> commonCards = cardRepository.findByRarity(Rarity.COMMON);

        if (commonCards.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_CARD_VALUE);
        }

        int totalCardsToCreate = 30;
        int cardIndex = 0;

        for (int i = 0; i < totalCardsToCreate; i++) {
            Card selectedCard = commonCards.get(cardIndex);

            Inventory inventory = new Inventory();
            inventory.setUserId(userId);
            inventory.setCardId(selectedCard.getCardId());
            inventory.setAcquiredAt(LocalDateTime.now());
            inventory.setIsForSale(false);
            inventory.setIsOnDeck(true);
            userCardRepository.save(inventory);

            cardIndex = (cardIndex + 1) % commonCards.size();
        }

        // Send welcome inventory notification
        sendWelcomeInventoryNotification(userId, totalCardsToCreate);
    }

    private void sendInventoryNotification(Long userId, Long cardId, Card card, String action, Long inventoryId) {
        try {
            NotificationMessage notification = new NotificationMessage();
            notification.setUserId(userId);
            notification.setTimestamp(System.currentTimeMillis());

            switch (action) {
                case "CARD_ADDED":
                    notification.setType("GACHA_RECEIVED");
                    notification.setMessage("Bạn đã nhận được thẻ từ gacha!");
                    break;
                case "CARD_REMOVED":
                    notification.setType("CARD_REMOVED");
                    notification.setMessage("Thẻ đã được xóa khỏi kho của bạn!");
                    break;
                case "CARD_PURCHASED":
                    notification.setType("CARD_PURCHASED");
                    notification.setMessage("Bạn đã mua thẻ thành công!");
                    break;
                default:
                    notification.setType("INVENTORY_UPDATE");
                    notification.setMessage("Kho đồ của bạn đã được cập nhật!");
                    break;
            }

            notificationKafkaTemplate.send("notification-events", notification);

            log.debug("Sent inventory notification for user: {}, action: {}", userId, action);

        } catch (Exception e) {
            log.error("Failed to send inventory notification: {}", e.getMessage());
        }
    }

    private void sendCardSaleNotification(Long userId, Long cardId, Card card, String action, Float price) {
        try {
            NotificationMessage notification = new NotificationMessage();
            notification.setUserId(userId);
            notification.setTimestamp(System.currentTimeMillis());

            switch (action) {
                case "CARD_LISTED_FOR_SALE":
                    notification.setType("CARD_LISTED");
                    notification.setMessage("Thẻ của bạn đã được đăng bán!");
                    break;
                case "CARD_SALE_CANCELLED":
                    notification.setType("SALE_CANCELLED");
                    notification.setMessage("Bạn đã hủy bán thẻ!");
                    break;
                case "CARD_SOLD":
                    notification.setType("CARD_SOLD");
                    notification.setMessage("Thẻ của bạn đã được bán thành công!");
                    break;
                default:
                    notification.setType("SALE_UPDATE");
                    notification.setMessage("Trạng thái bán thẻ đã được cập nhật!");
                    break;
            }

            notificationKafkaTemplate.send("notification-events", notification);

            log.debug("Sent card sale notification for user: {}, action: {}", userId, action);

        } catch (Exception e) {
            log.error("Failed to send card sale notification: {}", e.getMessage());
        }
    }

    private void sendWelcomeInventoryNotification(Long userId, int cardCount) {
        try {
            NotificationMessage notification = new NotificationMessage();
            notification.setUserId(userId);
            notification.setType("WELCOME_INVENTORY");
            notification.setMessage("Chào mừng! Bạn đã nhận được " + cardCount + " thẻ khởi đầu!");
            notification.setTimestamp(System.currentTimeMillis());

            notificationKafkaTemplate.send("notification-events", notification);

        } catch (Exception e) {
            log.error("Failed to send welcome inventory notification: {}", e.getMessage());
        }
    }

    // Async helper methods
    @Async
    public void saveAuditLogAsync(String action, String actorUsername, Long targetUserId, Long cardId, String description) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setAction(action);
            auditLog.setActorUsername(actorUsername);
            auditLog.setTargetUserId(targetUserId);
            auditLog.setCardId(cardId);
            auditLog.setDescription(description);
            auditLog.setTimestamp(LocalDateTime.now());
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage());
        }
    }

    @Async
    public void sendInventoryEventAsync(String action, Long userId, Long cardId, Long inventoryId, Float salePrice) {
        try {
            InventoryEvent event = new InventoryEvent();
            event.setAction(action);
            event.setUserId(userId);
            event.setCardId(cardId);
            event.setInventoryId(inventoryId);
            event.setSalePrice(salePrice);
            event.setTimestamp(LocalDateTime.now());
            inventoryKafkaTemplate.send("inventory-events", event);
        } catch (Exception e) {
            log.error("Failed to send inventory event: {}", e.getMessage());
        }
    }
}