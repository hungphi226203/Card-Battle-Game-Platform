package com.web_game.Notification_Service.Service;

import com.web_game.common.Event.InventoryEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "inventory-events", groupId = "notification-group", containerFactory = "kafkaListenerContainerFactory")
    public void handleInventoryEvent(InventoryEvent event) {
        messagingTemplate.convertAndSend("/topic/inventory/" + event.getUserId(), event);
    }
}