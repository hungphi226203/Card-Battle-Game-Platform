package com.web_game.Gateway_Service.Util;

import com.web_game.common.Enum.DeviceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@RequiredArgsConstructor
public class MultiDeviceWebSocketHandler implements WebSocketHandler {

    private final JwtUtil jwtUtil;

    // Map userId -> Map<deviceType, session>
    private final Map<Long, Map<DeviceType, WebSocketSession>> userDeviceSessions = new ConcurrentHashMap<>();

    // Map userId -> Map<deviceType, Sink> for handling outbound messages
    private final Map<Long, Map<DeviceType, Sinks.Many<String>>> userDeviceSinks = new ConcurrentHashMap<>();

    // Locks for thread-safe operations per user
    private final Map<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String token = extractToken(session);
        String clientInfo = session.getHandshakeInfo().getRemoteAddress().toString();

        log.info("=== NEW WEBSOCKET CONNECTION FROM {} ===", clientInfo);

        if (token == null || token.isBlank()) {
            log.warn("❌ Không có token từ {}", clientInfo);
            return session.close();
        }

        Long userId;
        DeviceType deviceType;
        try {
            userId = jwtUtil.getUserIdFromToken(token);
            deviceType = jwtUtil.getDeviceTypeFromToken(token);

            log.info("🔍 Parsed token - UserId: {}, DeviceType: {}", userId, deviceType.getValue());

        } catch (Exception e) {
            log.error("❌ Token validation failed từ {}: {}", clientInfo, e.getMessage());
            return session.close();
        }

        log.info("✅ User {} connected WebSocket from {} (Device: {})",
                userId, clientInfo, deviceType.getValue());

        // Create sink for this session
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

        // Thread-safe session management
        ReentrantLock userLock = userLocks.computeIfAbsent(userId, k -> new ReentrantLock());

        userLock.lock();
        try {
            // Lưu session theo userId và deviceType
            userDeviceSessions.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                    .put(deviceType, session);

            // Lưu sink cho outbound messages
            userDeviceSinks.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                    .put(deviceType, sink);

            // Log current sessions cho user này
            logUserSessions(userId);
        } finally {
            userLock.unlock();
        }

        // Handle outbound messages using the sink
        Mono<Void> output = session.send(
                sink.asFlux()
                        .map(session::textMessage)
                        .doOnNext(msg -> log.debug("📤 Sending to user {} ({}): {}",
                                userId, deviceType.getValue(), msg.getPayloadAsText()))
                        .doOnError(err -> log.error("❌ Error in outbound stream for user {} ({}): {}",
                                userId, deviceType.getValue(), err.getMessage()))
        );

        // Handle inbound messages
        Mono<Void> input = session.receive()
                .doOnNext(msg -> {
                    String payload = msg.getPayloadAsText();
                    log.info("📨 Message from user {} ({}): {}", userId, deviceType.getValue(), payload);
                })
                .doOnError(error -> {
                    log.error("❌ WebSocket error for user {} ({}): {}",
                            userId, deviceType.getValue(), error.getMessage());
                })
                .then();

        return Mono.zip(input, output)
                .doFinally(signal -> {
                    userLock.lock();
                    try {
                        // Complete the sink
                        sink.tryEmitComplete();

                        // Remove session and sink
                        Map<DeviceType, WebSocketSession> deviceSessions = userDeviceSessions.get(userId);
                        Map<DeviceType, Sinks.Many<String>> deviceSinks = userDeviceSinks.get(userId);

                        if (deviceSessions != null) {
                            deviceSessions.remove(deviceType);
                            if (deviceSessions.isEmpty()) {
                                userDeviceSessions.remove(userId);
                            }
                        }

                        if (deviceSinks != null) {
                            deviceSinks.remove(deviceType);
                            if (deviceSinks.isEmpty()) {
                                userDeviceSinks.remove(userId);
                            }
                        }

                        log.info("🔌 User {} disconnected WebSocket ({})", userId, deviceType.getValue());
                        logUserSessions(userId);
                    } finally {
                        userLock.unlock();
                        // Clean up lock if no more sessions for this user
                        if (!userDeviceSessions.containsKey(userId)) {
                            userLocks.remove(userId);
                        }
                    }
                })
                .then();
    }

    private String extractToken(WebSocketSession session) {
        URI uri = session.getHandshakeInfo().getUri();
        String query = uri.getQuery();

        if (query != null && query.contains("token=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    return param.substring(6);
                }
            }
        }

        String authHeader = session.getHandshakeInfo().getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }

    public void sendMessageToUser(Long userId, String message) {
        log.info("🚀 Attempting to send message to user {}: {}", userId, message);

        ReentrantLock userLock = userLocks.get(userId);
        if (userLock == null) {
            log.warn("⚠️ User {} has no active WebSocket sessions", userId);
            return;
        }

        userLock.lock();
        try {
            Map<DeviceType, Sinks.Many<String>> deviceSinks = userDeviceSinks.get(userId);
            if (deviceSinks == null || deviceSinks.isEmpty()) {
                log.warn("⚠️ User {} has no active WebSocket sessions", userId);
                return;
            }

            int successCount = 0;
            int totalSessions = deviceSinks.size();

            for (Map.Entry<DeviceType, Sinks.Many<String>> entry : deviceSinks.entrySet()) {
                DeviceType deviceType = entry.getKey();
                Sinks.Many<String> sink = entry.getValue();

                if (sink != null && !sink.tryEmitNext(message).isFailure()) {
                    log.info("✅ Queued message for user {} ({})", userId, deviceType.getValue());
                    successCount++;
                } else {
                    log.warn("⚠️ Failed to queue message for user {} ({})", userId, deviceType.getValue());
                }
            }

            log.info("📊 Message sent to user {}: {}/{} sessions successful",
                    userId, successCount, totalSessions);
        } finally {
            userLock.unlock();
        }
    }

    public void sendMessageToUserDevice(Long userId, DeviceType deviceType, String message) {
        log.info("🚀 Sending message to user {} on device {}: {}",
                userId, deviceType.getValue(), message);

        ReentrantLock userLock = userLocks.get(userId);
        if (userLock == null) {
            log.warn("⚠️ User {} has no WebSocket sessions", userId);
            return;
        }

        userLock.lock();
        try {
            Map<DeviceType, Sinks.Many<String>> deviceSinks = userDeviceSinks.get(userId);
            if (deviceSinks == null) {
                log.warn("⚠️ User {} has no WebSocket sessions", userId);
                return;
            }

            Sinks.Many<String> sink = deviceSinks.get(deviceType);
            if (sink != null && !sink.tryEmitNext(message).isFailure()) {
                log.info("✅ Successfully queued message for user {} ({})",
                        userId, deviceType.getValue());
            } else {
                log.warn("⚠️ No active session for user {} on device {} or failed to queue",
                        userId, deviceType.getValue());
            }
        } finally {
            userLock.unlock();
        }
    }

    private void logUserSessions(Long userId) {
        Map<DeviceType, WebSocketSession> sessions = userDeviceSessions.get(userId);
        if (sessions != null && !sessions.isEmpty()) {
            log.info("👤 User {} active sessions: {}", userId,
                    sessions.keySet().stream()
                            .map(DeviceType::getValue)
                            .toArray());
        }
    }

    public void debugActiveSessions() {
        log.info("=== ACTIVE WEBSOCKET SESSIONS ===");
        userDeviceSessions.forEach((userId, deviceSessions) -> {
            log.info("User {}: {} sessions", userId, deviceSessions.size());
            deviceSessions.forEach((deviceType, session) -> {
                log.info("  - {}: {} (Open: {})",
                        deviceType.getValue(), session.getId(), session.isOpen());
            });
        });
        log.info("Total users with active sessions: {}", userDeviceSessions.size());
        log.info("=== END SESSIONS ===");
    }
}