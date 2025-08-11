package com.web_game.Notification_Service.Client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "gateway-service", url = "${gateway.url}")
@Component
public interface WebSocketGatewayClient {

    @PostMapping("/internal/ws/push")
    void sendToUser(@RequestParam("userId") Long userId, @RequestParam("message") String message);

    @PostMapping("/internal/ws/push/{deviceType}")
    void sendToUserDevice(@RequestParam("userId") Long userId,
                          @PathVariable("deviceType") String deviceType,
                          @RequestParam("message") String message);

    @PostMapping("/internal/ws/push/unity")
    void sendToUnity(@RequestParam("userId") Long userId, @RequestParam("message") String message);
}