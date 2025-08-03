package com.web_game.Event_Service.Controller;

import com.web_game.Event_Service.Service.EventService;
import com.web_game.common.DTO.Respone.ApiResponse;
import com.web_game.common.DTO.Respone.GachaResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/event")
public class EventController {

    @Autowired
    private EventService eventService;

    private Long getUserIdFromHeader(HttpServletRequest request) {
        String userIdStr = request.getHeader("X-UserId");
        return userIdStr != null ? Long.parseLong(userIdStr) : null;
    }

    private boolean hasRole(HttpServletRequest request, String role) {
        String rolesHeader = request.getHeader("X-Roles");
        if (rolesHeader == null) return false;
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .anyMatch(r -> r.equalsIgnoreCase(role));
    }

    @PostMapping("/gacha/1")
    public ResponseEntity<ApiResponse> performSingleGacha(HttpServletRequest request) {
        Long userId = getUserIdFromHeader(request);
        if (userId == null) {
            throw new RuntimeException("Thiếu X-UserId trong header");
        }

        GachaResponse response = eventService.performSingleGacha(userId);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(201)
                .message("Quay 1 thẻ thành công")
                .data(response)
                .build());
    }

    @PostMapping("/gacha/10")
    public ResponseEntity<ApiResponse> performMultiGacha(HttpServletRequest request) {
        Long userId = getUserIdFromHeader(request);
        if (userId == null) {
            throw new RuntimeException("Thiếu X-UserId trong header");
        }

        List<GachaResponse> responses = eventService.performMultiGacha(userId);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(201)
                .message("Quay 10 thẻ thành công (có bảo hiểm Legendary)")
                .data(responses)
                .build());
    }
}