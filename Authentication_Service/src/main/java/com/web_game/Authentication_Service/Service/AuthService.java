package com.web_game.Authentication_Service.Service;

import com.web_game.common.DTO.Request.Auth.AuthRequest;
import com.web_game.common.DTO.Request.Auth.ChangePasswordRequest;
import com.web_game.common.Entity.User;
import com.web_game.common.DTO.Request.Auth.RegisterRequest;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public interface AuthService {
    User register(RegisterRequest request);
    String login(AuthRequest request);
    void logout(String token);
    String refreshToken(String refreshToken);
    Map<String, Object> verifyToken(String token);
    void changePassword(ChangePasswordRequest request, String token);
    void sendResetPasswordEmail(String email);
    void resetPassword(String resetToken, String newPassword);
}