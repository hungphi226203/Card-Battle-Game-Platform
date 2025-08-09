package com.web_game.Authentication_Service.Service.ServiceImpl;

import com.web_game.Authentication_Service.Service.SessionService;
import com.web_game.common.Enum.DeviceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String SESSION_PREFIX = "session";
    private static final String USER_SESSIONS_PREFIX = "user_sessions";

    /**
     * Generate session key: session:{username}:{deviceType}
     */
    private String getSessionKey(String username, DeviceType deviceType) {
        return String.format("%s:%s:%s", SESSION_PREFIX, username, deviceType.getValue());
    }

    /**
     * Generate user sessions key: user_sessions:{username}
     */
    private String getUserSessionsKey(String username) {
        return String.format("%s:%s", USER_SESSIONS_PREFIX, username);
    }

    @Override
    public void storeSession(String username, DeviceType deviceType, String token, long expirationMillis) {
        String sessionKey = getSessionKey(username, deviceType);
        String userSessionsKey = getUserSessionsKey(username);

        try {
            // Store the token
            redisTemplate.opsForValue().set(sessionKey, token, expirationMillis, TimeUnit.MILLISECONDS);

            // Track this device type for the user
            redisTemplate.opsForSet().add(userSessionsKey, deviceType.getValue());
            redisTemplate.expire(userSessionsKey, expirationMillis, TimeUnit.MILLISECONDS);

            log.info("Session stored for user: {} on device: {}", username, deviceType.getValue());
        } catch (Exception e) {
            log.error("Failed to store session for user: {} on device: {}", username, deviceType.getValue(), e);
            throw new RuntimeException("Failed to store session", e);
        }
    }

    @Override
    public String getSession(String username, DeviceType deviceType) {
        try {
            return redisTemplate.opsForValue().get(getSessionKey(username, deviceType));
        } catch (Exception e) {
            log.error("Failed to get session for user: {} on device: {}", username, deviceType.getValue(), e);
            return null;
        }
    }

    @Override
    public void removeSession(String username, DeviceType deviceType) {
        String sessionKey = getSessionKey(username, deviceType);
        String userSessionsKey = getUserSessionsKey(username);

        try {
            redisTemplate.delete(sessionKey);
            redisTemplate.opsForSet().remove(userSessionsKey, deviceType.getValue());

            log.info("Session removed for user: {} on device: {}", username, deviceType.getValue());
        } catch (Exception e) {
            log.error("Failed to remove session for user: {} on device: {}", username, deviceType.getValue(), e);
        }
    }

    @Override
    public void removeAllSessions(String username) {
        try {
            Set<DeviceType> activeDevices = getActiveDeviceTypes(username);

            for (DeviceType deviceType : activeDevices) {
                redisTemplate.delete(getSessionKey(username, deviceType));
            }

            redisTemplate.delete(getUserSessionsKey(username));

            log.info("All sessions removed for user: {}", username);
        } catch (Exception e) {
            log.error("Failed to remove all sessions for user: {}", username, e);
        }
    }

    @Override
    public boolean isTokenValid(String username, DeviceType deviceType, String token) {
        try {
            String storedToken = getSession(username, deviceType);
            return storedToken != null && storedToken.equals(token);
        } catch (Exception e) {
            log.error("Failed to validate token for user: {} on device: {}", username, deviceType.getValue(), e);
            return false;
        }
    }

    @Override
    public Set<DeviceType> getActiveDeviceTypes(String username) {
        try {
            Set<String> deviceTypeStrings = redisTemplate.opsForSet().members(getUserSessionsKey(username));

            if (deviceTypeStrings == null || deviceTypeStrings.isEmpty()) {
                return Set.of();
            }

            return deviceTypeStrings.stream()
                    .map(DeviceType::fromString)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Failed to get active device types for user: {}", username, e);
            return Set.of();
        }
    }
}