package com.web_game.Notification_Service.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // Config endpoint cho STOMP WebSocket
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")  // endpoint WebSocket
                .setAllowedOriginPatterns("*")
                .withSockJS();       // optional: nếu client dùng SockJS
    }

    // Cấu hình message broker
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic"); // broker nội bộ đơn giản
        registry.setApplicationDestinationPrefixes("/app"); // prefix client gửi lên server
    }
}
