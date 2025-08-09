package com.web_game.Authentication_Service.Service;

import com.web_game.common.Enum.DeviceType;
import org.springframework.stereotype.Service;

@Service
public interface SessionService {

    /**
     * Store session token for specific user and device type
     * This will override any existing session for the same user+deviceType
     */
    void storeSession(String username, DeviceType deviceType, String token, long expirationMillis);

    /**
     * Get active session token for user and device type
     */
    String getSession(String username, DeviceType deviceType);

    /**
     * Remove session for specific user and device type
     */
    void removeSession(String username, DeviceType deviceType);

    /**
     * Remove ALL sessions for a user (used in change password, etc.)
     */
    void removeAllSessions(String username);

    /**
     * Check if token is valid and matches stored session
     */
    boolean isTokenValid(String username, DeviceType deviceType, String token);

    /**
     * Get all active device types for a user
     */
    java.util.Set<DeviceType> getActiveDeviceTypes(String username);
}