package com.web_game.Notification_Service.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web_game.Notification_Service.Client.WebSocketGatewayClient;
import com.web_game.Notification_Service.Repository.NotificationRepository;
import com.web_game.Notification_Service.Repository.UserRepository;
import com.web_game.common.DTO.Request.Notification.NotificationRequest;
import com.web_game.common.DTO.Request.Notification.NotificationUpdateRequest;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketGatewayClient webSocketGatewayClient;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

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

    //ADMIN METHODS
    //lay tất cả thông báo cho quản lý thông báo
    public List<NotificationResponse> getAllNotifications() {
        List<Notification> notifications = notificationRepository.findGlobalNotifications();
        return notifications.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void createNotificationForAllUsers(NotificationRequest request) {
        try {
            log.info("Creating notification for all users: {}", request.getMessage());

            NotificationType type;
            try {
                type = NotificationType.valueOf(request.getType());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid notification type: {}, defaulting to SYSTEM_NOTIFICATION", request.getType());
                type = NotificationType.SYSTEM;
            }

            // Tạo groupId DUY NHẤT cho toàn bộ thông báo
            String groupId = UUID.randomUUID().toString();

            List<Long> allUserIds = userRepository.findAllUserIds();
            log.info("Found {} users to send notification", allUserIds.size());

            int batchSize = 100;
            for (int i = 0; i < allUserIds.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, allUserIds.size());
                List<Long> batch = allUserIds.subList(i, endIndex);

                processBatchNotifications(batch, type, request.getMessage(), groupId); // Truyền groupId vào
                log.debug("Processed batch {}-{} of {}", i + 1, endIndex, allUserIds.size());
            }

            log.info("Successfully created notification for all users with groupId: {}", groupId);
        } catch (Exception e) {
            log.error("Failed to create notification for all users: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create notification for all users", e);
        }
    }

    private void processBatchNotifications(List<Long> userIds, NotificationType type, String message, String groupId) {
        List<Notification> notifications = new ArrayList<>();

        for (Long userId : userIds) {
            Notification notification = new Notification();

            User user = new User();
            user.setUserId(userId);
            notification.setUser(user);

            notification.setType(type);
            notification.setMessage(message);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setRead(false);
            notification.setGroupId(groupId); // Sử dụng cùng groupId cho tất cả

            notifications.add(notification);
        }

        notificationRepository.saveAll(notifications);

        for (Long userId : userIds) {
            try {
                sendRealtimeNotification(userId, message, type.name());
            } catch (Exception e) {
                log.warn("Failed to send realtime notification to user {}: {}", userId, e.getMessage());
            }
        }
    }

    @Transactional
    public void updateNotification(NotificationUpdateRequest request) {
        String groupId = request.getGroupId();
        try {
            // Validate input
            if ((request.getMessage() == null || request.getMessage().trim().isEmpty())
                    && request.getType() == null) {
                throw new IllegalArgumentException("No fields to update");
            }

            String newMessage = null;
            if (request.getMessage() != null && !request.getMessage().trim().isEmpty()) {
                newMessage = request.getMessage().trim();
            }

            NotificationType newType = null;
            if (request.getType() != null) {
                try {
                    newType = NotificationType.valueOf(request.getType());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid notification type: {}, keeping current type", request.getType());
                }
            }

            // Update tất cả bản ghi có cùng groupId
            notificationRepository.updateByGroupId(groupId, newMessage, newType != null ? newType.name() : null);

            log.info("Updated all notifications with groupId {}", groupId);

            // Nếu cần có thể gửi WebSocket tới tất cả user liên quan
            // sendNotificationUpdateToGroup(groupId);

        } catch (Exception e) {
            log.error("Failed to update notifications with groupId {}: {}", groupId, e.getMessage(), e);
            throw new RuntimeException("Failed to update notifications", e);
        }
    }

    @Transactional
    public void deleteByGroupId(String groupId) {
        if (groupId == null || groupId.trim().isEmpty()) {
            throw new IllegalArgumentException("groupId không được để trống");
        }

        int deletedCount = notificationRepository.deleteByGroupId(groupId);

        if (deletedCount == 0) {
            throw new RuntimeException("Không tìm thấy thông báo nào với groupId: " + groupId);
        }

        log.info("Đã xóa {} thông báo với groupId {}", deletedCount, groupId);
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
                return NotificationType.SYSTEM;
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