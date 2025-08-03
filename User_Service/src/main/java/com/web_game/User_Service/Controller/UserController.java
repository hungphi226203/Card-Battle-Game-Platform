package com.web_game.User_Service.Controller;

import com.web_game.User_Service.Service.UserService;
import com.web_game.common.DTO.Request.User.RoleAssignmentRequest;
import com.web_game.common.DTO.Request.User.UserUpdateRequest;
import com.web_game.common.DTO.Respone.ApiResponse;
import com.web_game.common.DTO.shared.UserDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    @Autowired
    private UserService userService;

    private boolean hasRole(HttpServletRequest request, String role) {
        String rolesHeader = request.getHeader("X-Roles");
        if (rolesHeader == null) return false;
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .anyMatch(r -> r.equalsIgnoreCase(role));
    }

    private Long getUserIdFromHeader(HttpServletRequest request) {
        String userIdStr = request.getHeader("X-UserId");
        return userIdStr != null ? Long.parseLong(userIdStr) : null;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse> getCurrentUser(HttpServletRequest request) {
        Long userId = getUserIdFromHeader(request);
        if (userId == null) {
            throw new AccessDeniedException("Thiếu X-UserId trong header");
        }
        UserDTO user = userService.getCurrentUserById(userId);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Lấy thông tin cá nhân thành công")
                .data(user)
                .build());
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse> updateCurrentUser(@Valid @RequestBody UserUpdateRequest request,
                                                         HttpServletRequest httpRequest) {
        Long userId = getUserIdFromHeader(httpRequest);
        if (userId == null) {
            throw new AccessDeniedException("Thiếu X-UserId trong header");
        }
        UserDTO user = userService.updateCurrentUserById(request, userId);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Cập nhật thông tin cá nhân thành công")
                .data(user)
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getAllUsers(HttpServletRequest request) {
        if (!hasRole(request, "ADMIN") && !hasRole(request, "MANAGER")) {
            throw new AccessDeniedException("Bạn không có quyền truy cập danh sách user");
        }
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Lấy danh sách user thành công")
                .data(users)
                .build());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse> getUser(@PathVariable Long userId,
                                               HttpServletRequest request) {
        Long currentUserId = getUserIdFromHeader(request);
        if (!hasRole(request, "ADMIN") && !hasRole(request, "MANAGER") && !userId.equals(currentUserId)) {
            throw new AccessDeniedException("Bạn không có quyền xem thông tin user này");
        }
        UserDTO user = userService.getUser(userId);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Lấy thông tin user thành công")
                .data(user)
                .build());
    }

    @PutMapping("/{userId}/lock")
    public ResponseEntity<ApiResponse> lockUser(@PathVariable Long userId,
                                                HttpServletRequest request) {
        Long currentUserId = getUserIdFromHeader(request);
        if ((!hasRole(request, "ADMIN") && !hasRole(request, "MANAGER")) || userId.equals(currentUserId)) {
            throw new AccessDeniedException("Bạn không thể khóa tài khoản này");
        }
        userService.lockUser(userId);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Khóa tài khoản thành công")
                .data(null)
                .build());
    }

    @PutMapping("/{userId}/unlock")
    public ResponseEntity<ApiResponse> unlockUser(@PathVariable Long userId,
                                                  HttpServletRequest request) {
        Long currentUserId = getUserIdFromHeader(request);
        if ((!hasRole(request, "ADMIN") && !hasRole(request, "MANAGER")) || userId.equals(currentUserId)) {
            throw new AccessDeniedException("Bạn không thể mở khóa tài khoản này");
        }
        userService.unlockUser(userId);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Mở khóa tài khoản thành công")
                .data(null)
                .build());
    }

    @PostMapping("/{userId}/roles")
    public ResponseEntity<ApiResponse> assignRole(@PathVariable Long userId,
                                                  @Valid @RequestBody RoleAssignmentRequest request,
                                                  HttpServletRequest httpRequest) {
        Long currentUserId = getUserIdFromHeader(httpRequest);
        if (!hasRole(httpRequest, "ADMIN") || userId.equals(currentUserId)) {
            throw new AccessDeniedException("Bạn không có quyền gán role");
        }
        userService.assignRole(userId, request.getRoleName());
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Gán vai trò thành công")
                .data(null)
                .build());
    }

    @DeleteMapping("/{userId}/roles/{roleName}")
    public ResponseEntity<ApiResponse> removeRole(@PathVariable Long userId,
                                                  @PathVariable String roleName,
                                                  HttpServletRequest httpRequest) {
        Long currentUserId = getUserIdFromHeader(httpRequest);
        if (!hasRole(httpRequest, "ADMIN") || userId.equals(currentUserId)) {
            throw new AccessDeniedException("Bạn không có quyền xóa role");
        }
        userService.removeRole(userId, roleName);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Xóa vai trò thành công")
                .data(null)
                .build());
    }
}