package com.web_game.Gateway_Service.Controller;

import com.web_game.Gateway_Service.Util.MultiDeviceWebSocketHandler;
import com.web_game.common.Enum.DeviceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/internal/ws")
@RequiredArgsConstructor
public class MultiDeviceWebSocketController {

    private final MultiDeviceWebSocketHandler handler;

    /**
     * Gửi message đến TẤT CẢ devices của user
     */
    @PostMapping("/push")
    public String pushToUser(@RequestParam Long userId, @RequestParam String message) {
        log.info("Push message to ALL devices of user {}: {}", userId, message);

        try {
            handler.sendMessageToUser(userId, message);
            return "Message sent to all devices of user " + userId;
        } catch (Exception e) {
            log.error("Error pushing message to user {}: {}", userId, e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Gửi message đến device cụ thể của user
     */
    @PostMapping("/push/{deviceType}")
    public String pushToUserDevice(
            @RequestParam Long userId,
            @PathVariable String deviceType,
            @RequestParam String message) {

        log.info("Push message to user {} on device {}: {}", userId, deviceType, message);

        try {
            DeviceType device = DeviceType.fromString(deviceType);
            handler.sendMessageToUserDevice(userId, device, message);
            return String.format("Message sent to user %d on device %s", userId, deviceType);
        } catch (Exception e) {
            log.error("Error pushing message to user {} on device {}: {}",
                    userId, deviceType, e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/push/unity")
    public String pushToUnity(@RequestParam Long userId, @RequestParam String message) {
        log.info("Push message to Unity devices of user {}: {}", userId, message);

        try {
            // Chỉ gửi đến UNITY device
            handler.sendMessageToUserDevice(userId, DeviceType.UNITY, message);
            return "Message sent to Unity device of user " + userId;
        } catch (Exception e) {
            log.error("Error pushing message to Unity for user {}: {}", userId, e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/debug/sessions")
    public String getActiveSessions() {
        handler.debugActiveSessions();
        return "Check logs for session information";
    }
}