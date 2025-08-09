package com.web_game.Notification_Service.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web_game.Notification_Service.Client.WebSocketGatewayClient;
import com.web_game.common.Event.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedNotificationKafkaConsumer {

    private final WebSocketGatewayClient webSocketClient;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "notification-events",
            groupId = "notification-service",
            containerFactory = "notificationKafkaListenerContainerFactory"
    )
    public void handleNotificationEvent(String messageJson) {
        try {
            log.debug("Received notification event: {}", messageJson);

            NotificationMessage notification = objectMapper.readValue(messageJson, NotificationMessage.class);

            // Validate notification
            if (notification.getUserId() == null || notification.getMessage() == null) {
                log.warn("Invalid notification message: {}", messageJson);
                return;
            }

            // Forward to WebSocket Gateway - Fixed: Use Long userId
            webSocketClient.sendToUser(notification.getUserId(), notification.getMessage());

            log.info("Successfully forwarded notification to user {} (type: {})",
                    notification.getUserId(), notification.getType());

        } catch (Exception e) {
            log.error("Failed to process notification event: {} - Error: {}", messageJson, e.getMessage());
        }
    }

    @KafkaListener(
            topics = "inventory-events",
            groupId = "notification-inventory-group",
            containerFactory = "notificationKafkaListenerContainerFactory"
    )
    public void handleInventoryEvent(String eventJson) {
        try {
            log.debug("Received inventory event: {}", eventJson);

            log.info("Processed inventory event: {}", eventJson);

        } catch (Exception e) {
            log.error("Failed to process inventory event: {} - Error: {}", eventJson, e.getMessage());
        }
    }

    @KafkaListener(
            topics = "gacha-events",
            groupId = "notification-gacha-group",
            containerFactory = "notificationKafkaListenerContainerFactory"
    )
    public void handleGachaEvent(String eventJson) {
        try {
            log.debug("Received gacha event: {}", eventJson);

            NotificationMessage notification = objectMapper.readValue(eventJson, NotificationMessage.class);

            if (notification.getUserId() == null || notification.getMessage() == null) {
                log.warn("Invalid gacha notification: {}", eventJson);
                return;
            }

            webSocketClient.sendToUnity(notification.getUserId(), notification.getMessage());

            log.info("Successfully forwarded gacha notification to Unity for user {}",
                    notification.getUserId());

        } catch (Exception e) {
            log.error("Failed to process gacha event: {} - Error: {}", eventJson, e.getMessage());
        }
    }
}