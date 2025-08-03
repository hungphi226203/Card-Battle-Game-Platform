package com.web_game.Authentication_Service.Service;

import com.web_game.common.DTO.Request.Auth.ChangePasswordRequest;
import com.web_game.common.Entity.User;
import com.web_game.common.DTO.Request.Auth.RegisterRequest;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public interface AuthService {

    public User register(RegisterRequest request);

    public String login(String username, String password);

    public void logout(String token);

    public Map<String, Object> verifyToken(String token);

    public String refreshToken(String refreshToken);

    public void changePassword(ChangePasswordRequest request, String token);

    public void sendResetPasswordEmail(String email);

    public void resetPassword(String resetToken, String newPassword);

    public User getCurrentUser(String token);

}