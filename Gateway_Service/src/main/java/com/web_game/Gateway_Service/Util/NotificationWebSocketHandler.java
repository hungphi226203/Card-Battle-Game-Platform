package com.web_game.Gateway_Service.Util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler implements WebSocketHandler {

    private final JwtUtil jwtUtil;

    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String uri = session.getHandshakeInfo().getUri().toString();

        String token = null;
        if (uri.contains("token=")) {
            token = uri.substring(uri.indexOf("token=") + 6);
        }

        if (token == null || token.isBlank()) {
            log.warn("Không có token trong kết nối WebSocket");
            return session.close();
        }

        Long userId;
        try {
            userId = jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            log.warn("Token không hợp lệ: {}", e.getMessage());
            return session.close();
        }

        log.info("User {} đã kết nối WebSocket", userId);
        userSessions.put(userId, session);

        return session.receive()
                .doOnNext(msg -> log.info("Nhận từ user {}: {}", userId, msg.getPayloadAsText()))
                .doFinally(signal -> {
                    userSessions.remove(userId);
                    log.info("User {} ngắt kết nối WebSocket", userId);
                })
                .then();
    }

    public void sendMessageToUser(Long userId, String message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            session.send(Mono.just(session.textMessage(message)))
                    .subscribe(null, err -> log.error("Lỗi gửi tin: {}", err.getMessage()));
        } else {
            log.warn("User {} không có session WebSocket đang mở", userId);
        }
    }
}