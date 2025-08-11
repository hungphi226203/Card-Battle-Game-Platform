package com.web_game.Notification_Service.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web_game.Notification_Service.Client.WebSocketGatewayClient;
import com.web_game.Notification_Service.Repository.NotificationRepository;
import com.web_game.common.DTO.Respone.NotificationResponse;
import com.web_game.common.Entity.Notification;
import com.web_game.common.Entity.User;
import com.web_game.common.Enum.NotificationType;
import com.web_game.common.Event.InventoryEvent;
import com.web_game.common.Event.NotificationMessage;
import com.web_game.common.Event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketGatewayClient webSocketGatewayClient;
    private final ObjectMapper objectMapper;

    // ============= KAFKA LISTENERS =============

    @KafkaListener(
            topics = "inventory-events",
            groupId = "notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleInventoryEvent(InventoryEvent event) {
        log.info("Handling inventory event: {}", event.getAction());

        if ("TRANSFER_CARD".equals(event.getAction())) {
            log.info("Ignoring TRANSFER_CARD event for notification");
            return;
        }

        try {
            String message = buildInventoryMessage(event);
            NotificationType type = mapInventoryEventToNotificationType(event.getAction());

            saveNotification(event.getUserId(), type, message, null);
            sendRealtimeNotification(event.getUserId(), message, type.name());

        } catch (Exception e) {
            log.error("Failed to handle inventory event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = "transaction-events",
            groupId = "notification-transaction-group",
            containerFactory = "transactionKafkaListenerContainerFactory"
    )
    public void handleTransactionEvent(TransactionEvent event) {
        try {
            log.info("Handling transaction event: {}", event);

            String action = event.getAction();
            Long buyerId = event.getBuyerId();
            Long sellerId = event.getSellerId();
            Float price = event.getPrice();

            if ("COMPLETE_TRANSACTION".equals(action)) {
                String buyerMessage = "Bạn đã mua thẻ thành công với giá " + price + " xu!";
                saveNotification(buyerId, NotificationType.TRANSACTION_SUCCESS, buyerMessage, null);
                sendRealtimeNotification(buyerId, buyerMessage, "TRANSACTION_SUCCESS");

                String sellerMessage = "Thẻ của bạn đã được bán thành công với giá " + price + " xu!";
                saveNotification(sellerId, NotificationType.TRANSACTION_SUCCESS, sellerMessage, null);
                sendRealtimeNotification(sellerId, sellerMessage, "TRANSACTION_SUCCESS");
            }
        } catch (Exception e) {
            log.error("Failed to handle transaction event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = "gacha-events",
            groupId = "notification-gacha-group",
            containerFactory = "gachaKafkaListenerContainerFactory"
    )
    public void handleGachaEvent(String eventJson) {
        try {
            log.info("Handling gacha event: {}", eventJson);

            Map<String, Object> eventMap = objectMapper.readValue(eventJson, Map.class);
            Long userId = Long.valueOf(eventMap.get("userId").toString());
            String cardName = (String) eventMap.getOrDefault("cardName", "Unknown Card");
            String rarity = (String) eventMap.getOrDefault("rarity", "COMMON");

            String message = "Chúc mừng! Bạn đã nhận được thẻ " + cardName + " (" + rarity + ") từ gacha!";

            sendRealtimeNotification(userId, message, "GACHA_REWARD");

        } catch (Exception e) {
            log.error("Failed to handle gacha event: {}", e.getMessage(), e);
        }
    }


    // ============= PUBLIC API METHODS =============

    public Page<NotificationResponse> getUserNotifications(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationRepository.findByUserUserIdOrderByCreatedAtDesc(userId, pageable);

        return notifications.map(this::toResponse);
    }

    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        List<Notification> unreadNotifications = notificationRepository.findByUserUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);

        return unreadNotifications.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.markAsReadByIdAndUserId(notificationId, userId);

        // Gửi cập nhật số lượng unread qua WebSocket
        long unreadCount = getUnreadCount(userId);
        sendUnreadCountUpdate(userId, unreadCount);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);

        // Gửi cập nhật số lượng unread = 0
        sendUnreadCountUpdate(userId, 0);
    }

    // ============= PRIVATE HELPER METHODS =============

    private void saveNotification(Long userId, NotificationType type, String message, Long transactionId) {
        try {
            Notification notification = new Notification();

            User user = new User();
            user.setUserId(userId);
            notification.setUser(user);

            notification.setType(type);
            notification.setMessage(message);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setRead(false);

            if (transactionId != null) {
                // Set transaction if needed
            }

            notificationRepository.save(notification);
            log.debug("Saved notification for user {}: {}", userId, message);

        } catch (Exception e) {
            log.error("Failed to save notification: {}", e.getMessage(), e);
        }
    }

    private void sendRealtimeNotification(Long userId, String message, String type) {
        try {
            NotificationMessage realtimeNotification = new NotificationMessage();
            realtimeNotification.setUserId(userId);
            realtimeNotification.setMessage(message);
            realtimeNotification.setType(type);
            realtimeNotification.setTimestamp(System.currentTimeMillis());

            String notificationJson = objectMapper.writeValueAsString(realtimeNotification);

            // Gửi qua WebSocket gateway
            webSocketGatewayClient.sendToUser(userId, notificationJson);

            // Backup: Gửi qua internal WebSocket (nếu trong cùng service)
            messagingTemplate.convertAndSend("/topic/notifications/" + userId, realtimeNotification);

            log.debug("Sent realtime notification to user {}: {}", userId, message);

        } catch (Exception e) {
            log.error("Failed to send realtime notification: {}", e.getMessage(), e);
        }
    }

    private void sendUnreadCountUpdate(Long userId, long unreadCount) {
        try {
            Map<String, Object> countUpdate = Map.of(
                    "type", "UNREAD_COUNT_UPDATE",
                    "unreadCount", unreadCount,
                    "timestamp", System.currentTimeMillis()
            );

            String countJson = objectMapper.writeValueAsString(countUpdate);
            webSocketGatewayClient.sendToUser(userId, countJson);

            // Backup internal
            messagingTemplate.convertAndSend("/topic/notifications/" + userId + "/count", countUpdate);

        } catch (Exception e) {
            log.error("Failed to send unread count update: {}", e.getMessage(), e);
        }
    }

    private String buildInventoryMessage(InventoryEvent event) {
        switch (event.getAction()) {
            case "ADD_CARD":
                return "Bạn đã nhận được thẻ mới vào kho!";
            case "REMOVE_CARD":
                return "Thẻ đã được xóa khỏi kho của bạn!";
            case "LIST_CARD_FOR_SALE":
                return "Thẻ của bạn đã được đăng bán thành công!";
            case "CANCEL_CARD_SALE":
                return "Bạn đã hủy rao bán thẻ!";
            // Bỏ case "TRANSFER_CARD"
            default:
                return "Kho đồ của bạn đã được cập nhật!";
        }
    }

    private NotificationType mapInventoryEventToNotificationType(String action) {
        switch (action) {
            case "ADD_CARD":
            case "GACHA_CARD":
                return NotificationType.INVENTORY_UPDATE;
            case "LIST_CARD_FOR_SALE":
            case "CANCEL_CARD_SALE":
                return NotificationType.MARKET_ACTIVITY;
            // Bỏ case "TRANSFER_CARD"
            default:
                return NotificationType.SYSTEM_NOTIFICATION;
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        BeanUtils.copyProperties(notification, response);
        response.setType(notification.getType().name());
        response.setUserId(notification.getUser().getUserId());
        return response;
    }
}