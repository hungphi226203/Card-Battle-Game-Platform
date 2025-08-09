package com.web_game.Authentication_Service.Controller;

import com.web_game.Authentication_Service.Service.AuthService;
import com.web_game.common.DTO.Request.Auth.*;
import com.web_game.common.DTO.Respone.ApiResponse;
import com.web_game.common.Entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Đăng ký thành công")
                .data(user)
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody AuthRequest request) {
        String token = authService.login(request);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Đăng nhập thành công")
                .data(token)
                .build());
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse> verify(@RequestBody VerifyRequest request) {
        var userDetails = authService.verifyToken(request.getToken());
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Token hợp lệ")
                .data(userDetails)
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@RequestHeader("Authorization") String authHeader) {
        authService.logout(authHeader.replace("Bearer ", ""));
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Đăng xuất thành công")
                .data(null)
                .build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String newToken = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Làm mới token thành công")
                .data(newToken)
                .build());
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                      @RequestHeader("Authorization") String authHeader) {
        authService.changePassword(request, authHeader.replace("Bearer ", ""));
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Đổi mật khẩu thành công")
                .data(null)
                .build());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.sendResetPasswordEmail(request.getEmail());
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Đã gửi email reset mật khẩu")
                .data(null)
                .build());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Đặt lại mật khẩu thành công")
                .data(null)
                .build());
    }
}
