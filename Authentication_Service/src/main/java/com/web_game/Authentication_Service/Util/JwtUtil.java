package com.web_game.Authentication_Service.Util;

import com.web_game.common.Entity.User;
import com.web_game.common.Enum.DeviceType;
import com.web_game.common.Exception.AppException;
import com.web_game.common.Exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    public String generateToken(User user, List<String> roles, DeviceType deviceType, String deviceId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("roles", roles);
        claims.put("deviceType", deviceType.getValue());
        claims.put("deviceId", deviceId != null ? deviceId : "default");
        return createToken(claims, user.getUsername(), expiration);
    }

    // Backward compatibility
    public String generateToken(User user, List<String> roles) {
        return generateToken(user, roles, DeviceType.WEB, "default");
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateRefreshToken(User user, DeviceType deviceType, String deviceId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("deviceType", deviceType.getValue());
        claims.put("deviceId", deviceId != null ? deviceId : "default");
        return createToken(claims, user.getUsername(), refreshExpiration);
    }

    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    public String getUsernameFromToken(String token) {
        return validateToken(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        return validateToken(token).get("userId", Long.class);
    }

    public List<String> getRolesFromToken(String token) {
        return validateToken(token).get("roles", List.class);
    }

    public DeviceType getDeviceTypeFromToken(String token) {
        String deviceTypeStr = validateToken(token).get("deviceType", String.class);
        return deviceTypeStr != null ? DeviceType.fromString(deviceTypeStr) : DeviceType.WEB;
    }

    public String getDeviceIdFromToken(String token) {
        return validateToken(token).get("deviceId", String.class);
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = validateToken(token).getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}