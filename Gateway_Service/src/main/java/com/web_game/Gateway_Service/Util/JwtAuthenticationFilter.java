package com.web_game.Gateway_Service.Util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final String secretKey;
    private final StringRedisTemplate redisTemplate;

    public JwtAuthenticationFilter(@Value("${jwt.secret}") String secretKey,
                                   StringRedisTemplate redisTemplate) {
        this.secretKey = secretKey;
        this.redisTemplate = redisTemplate;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (path.startsWith("/auth/") || path.startsWith("/ws")) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return handleUnauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        try {
            // Parse JWT
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();
            Object userIdObj = claims.get("userId");
            List<String> roles = claims.get("roles", List.class);
            String deviceType = claims.get("deviceType", String.class);

            // Check session in Redis using deviceType
            String sessionKey = String.format("session:%s:%s", username, deviceType != null ? deviceType : "web");
            String sessionToken = redisTemplate.opsForValue().get(sessionKey);

            if (sessionToken == null || !sessionToken.equals(token)) {
                log.warn("Token for user {} on device {} has been revoked or expired", username, deviceType);
                return handleUnauthorized(exchange, "Token revoked or expired");
            }

            // Add custom headers for downstream services
            Long userId = Long.valueOf(userIdObj.toString());
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-UserId", userId.toString())
                    .header("X-Username", username)
                    .header("X-Roles", String.join(",", roles))
                    .header("X-DeviceType", deviceType != null ? deviceType : "web")
                    .build();

            ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
            return chain.filter(mutatedExchange);

        } catch (Exception e) {
            log.error("JWT validation failed for path {}: {}", path, e.getMessage());
            return handleUnauthorized(exchange, "Invalid or expired token");
        }
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        String body = String.format("{\"code\":401,\"message\":\"Unauthorized - %s\",\"data\":null}", message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}