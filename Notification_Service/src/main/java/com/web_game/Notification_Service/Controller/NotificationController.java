package com.web_game.Notification_Service.Controller;

import com.web_game.Notification_Service.Service.NotificationService;
import com.web_game.common.DTO.Respone.ApiResponse;
import com.web_game.common.DTO.Respone.NotificationResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    private Long getUserIdFromHeader(HttpServletRequest request) {
        String userIdStr = request.getHeader("X-UserId");
        return userIdStr != null ? Long.parseLong(userIdStr) : null;
    }

    // Lấy danh sách notification có phân trang
    @GetMapping
    public ResponseEntity<ApiResponse> getNotifications(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long userId = getUserIdFromHeader(request);
        if (userId == null) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .code(400)
                            .message("Thiếu X-UserId trong header")
                            .build());
        }

        Page<NotificationResponse> notifications = notificationService.getUserNotifications(userId, page, size);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Lấy danh sách thông báo thành công")
                .data(notifications)
                .build());
    }

    // Lấy danh sách notification chưa đọc
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse> getUnreadNotifications(HttpServletRequest request) {
        Long userId = getUserIdFromHeader(request);
        if (userId == null) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .code(400)
                            .message("Thiếu X-UserId trong header")
                            .build());
        }

        List<NotificationResponse> unreadNotifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Lấy thông báo chưa đọc thành công")
                .data(unreadNotifications)
                .build());
    }

    // Lấy số lượng notification chưa đọc
    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse> getUnreadCount(HttpServletRequest request) {
        Long userId = getUserIdFromHeader(request);
        if (userId == null) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .code(400)
                            .message("Thiếu X-UserId trong header")
                            .build());
        }

        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Lấy số lượng thông báo chưa đọc thành công")
                .data(count)
                .build());
    }

    // Đánh dấu 1 notification đã đọc
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse> markAsRead(
            @PathVariable Long notificationId,
            HttpServletRequest request) {

        Long userId = getUserIdFromHeader(request);
        if (userId == null) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .code(400)
                            .message("Thiếu X-UserId trong header")
                            .build());
        }

        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Đánh dấu thông báo đã đọc thành công")
                .build());
    }

    // Đánh dấu tất cả notification đã đọc
    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse> markAllAsRead(HttpServletRequest request) {

        Long userId = getUserIdFromHeader(request);
        if (userId == null) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .code(400)
                            .message("Thiếu X-UserId trong header")
                            .build());
        }

        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Đánh dấu tất cả thông báo đã đọc thành công")
                .build());
    }
}
