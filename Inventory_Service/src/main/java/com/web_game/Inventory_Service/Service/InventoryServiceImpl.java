package com.web_game.Inventory_Service.Service;

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
import com.web_game.common.Event.TransactionEvent;
import com.web_game.common.Event.UserRegisteredEvent;
import com.web_game.common.Exception.AppException;
import com.web_game.common.Exception.ErrorCode;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private UserCardRepository userCardRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private KafkaTemplate<String, InventoryEvent> kafkaTemplate;

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
        Inventory inventory = new Inventory();
        inventory.setUserId(userId);
        inventory.setCardId(cardId);
        inventory.setAcquiredAt(LocalDateTime.now());
        inventory.setIsForSale(false);
        inventory.setIsOnDeck(false);
        userCardRepository.save(inventory);

        saveAuditLogAsync(
                actorUsername.equals("SYSTEM") ? AuditAction.GACHA_CARD : AuditAction.ADD_CARD,
                actorUsername,
                userId,
                cardId,
                actorUsername.equals("SYSTEM") ?
                        "Thêm thẻ " + cardId + " vào kho của user " + userId + " qua gacha" :
                        "Thêm thẻ " + cardId + " vào kho của user " + userId + " bởi " + actorUsername
        );

        sendInventoryEventAsync(
                actorUsername.equals("SYSTEM") ? AuditAction.GACHA_CARD : AuditAction.ADD_CARD,
                userId,
                cardId,
                inventory.getInventoryId(),
                null
        );
    }

    @Transactional
    public void removeCardFromInventory(Long userId, Long cardId, String actorUsername) {
        Inventory inventory = userCardRepository.findByUserUserIdAndCardCardId(userId, cardId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_CARD_NOT_FOUND));
        userCardRepository.delete(inventory);

        saveAuditLogAsync(AuditAction.REMOVE_CARD, actorUsername, userId, cardId,
                "Xóa thẻ " + cardId + " khỏi kho của user " + userId + " bởi " + actorUsername);

        sendInventoryEventAsync(AuditAction.REMOVE_CARD, userId, cardId, inventory.getInventoryId(), null);
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
    }

    @Autowired
    private UserRepository userRepository;

    public List<InventoryResponse> getCardsForSale() {
        return userCardRepository.findByIsForSaleTrueAndIsOnDeckFalse().stream()
                .map(inventory -> {
                    InventoryResponse response = new InventoryResponse();
                    BeanUtils.copyProperties(inventory, response);

                    // Lấy thông tin thẻ
                    cardRepository.findById(inventory.getCardId()).ifPresent(card -> {
                        response.setCardName(card.getName());
                        response.setImageUrl(card.getImageUrl());
                        response.setMainImg(card.getMainImg());
                        response.setRarity(card.getRarity());
                        response.setType(card.getType());
                    });

                    // ✅ Lấy tên người bán
                    userRepository.findById(inventory.getUserId()).ifPresent(user -> {
                        response.setSellerName(user.getFullName()); // hoặc user.getUsername()
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

                    // Lấy thông tin thẻ
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
    public void handleGachaEvent(GachaEvent event) {
        addCardToInventory(event.getUserId(), event.getCardId(), "SYSTEM");
    }

    @KafkaListener(topics = "transaction-events", groupId = "inventory-group", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void handleTransactionEvent(TransactionEvent event) {
        if (AuditAction.COMPLETE_TRANSACTION.equals(event.getAction())) {

            Inventory inventory = userCardRepository.findById(event.getInventoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_CARD_NOT_FOUND));

            inventory.setUserId(event.getBuyerId());
            inventory.setIsForSale(false);
            inventory.setIsOnDeck(false);
            inventory.setAcquiredAt(LocalDateTime.now());
            userCardRepository.save(inventory);

            saveAuditLogAsync(AuditAction.COMPLETE_TRANSACTION, "SYSTEM", event.getBuyerId(),
                    inventory.getCardId(), "Ownership updated via Kafka for inventory " + inventory.getInventoryId());

            sendInventoryEventAsync(AuditAction.TRANSFER_CARD, event.getBuyerId(), inventory.getCardId(),
                    inventory.getInventoryId(), null);
        }
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
    public void sendInventoryEventAsync(String action, Long userId, Long cardId, Long inventoryId, Float salePrice) {
        InventoryEvent event = new InventoryEvent();
        event.setAction(action);
        event.setUserId(userId);
        event.setCardId(cardId);
        event.setInventoryId(inventoryId);
        event.setSalePrice(salePrice);
        event.setTimestamp(LocalDateTime.now());
        kafkaTemplate.send("inventory-events", event);
    }

    @KafkaListener(topics = "user-registered-topic", groupId = "inventory-group", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void handleUserRegistered(UserRegisteredEvent event) {
        Long userId = event.getUserId();

        List<Card> commonCards = cardRepository.findByRarity(Rarity.COMMON);

        if (commonCards.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_CARD_VALUE);
        }

        for (Card card : commonCards) {
            for (int i = 0; i < 6; i++) {
                Inventory inventory = new Inventory();
                inventory.setUserId(userId);
                inventory.setCardId(card.getCardId());
                inventory.setAcquiredAt(LocalDateTime.now());
                inventory.setIsForSale(false);
                inventory.setIsOnDeck(true);
                userCardRepository.save(inventory);
            }
        }

        saveAuditLogAsync("INIT_INVENTORY", "SYSTEM", userId, null, "Khởi tạo 30 thẻ COMMON vào kho inventory");
    }
}