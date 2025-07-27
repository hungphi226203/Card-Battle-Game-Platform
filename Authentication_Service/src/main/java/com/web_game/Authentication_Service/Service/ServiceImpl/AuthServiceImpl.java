package com.web_game.Authentication_Service.Service.ServiceImpl;

import com.web_game.Authentication_Service.Repository.RoleRepository;
import com.web_game.Authentication_Service.Repository.UserRepository;
import com.web_game.Authentication_Service.Repository.UserRoleRepository;
import com.web_game.Authentication_Service.Service.AuthService;
import com.web_game.Authentication_Service.Service.SessionService;
import com.web_game.common.DTO.Request.Auth.ChangePasswordRequest;
import com.web_game.common.DTO.Request.Auth.RegisterRequest;
import com.web_game.common.Entity.Role;
import com.web_game.common.Entity.User;
import com.web_game.common.Entity.UserRole;
import com.web_game.common.Enum.RoleName;
import com.web_game.common.Event.UserRegisteredEvent;
import com.web_game.common.Exception.AppException;
import com.web_game.common.Exception.ErrorCode;
import com.web_game.Authentication_Service.Util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRoleRepository userRoleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (request.getDob() != null && request.getDob().isAfter(LocalDate.now().minusYears(13))) {
            throw new AppException(ErrorCode.INVALID_DOB);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setGender(request.getGender());
        user.setDob(request.getDob());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        Role userRole = roleRepository.findByRoleName(RoleName.USER)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));
        UserRole userRoleEntity = new UserRole();
        userRoleEntity.setUserId(user.getUserId());
        userRoleEntity.setRoleId(userRole.getRoleId());
        userRoleEntity.setAssignedAt(LocalDateTime.now());
        userRoleRepository.save(userRoleEntity);

        try {
            UserRegisteredEvent event = new UserRegisteredEvent(user.getUserId());
            kafkaTemplate.send("user-registered-topic", event);
        } catch (Exception e) {
            System.err.println("Failed to publish UserRegistered event: " + e.getMessage());
        }

        return user;
    }

    public String login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Fixed: Get roles properly and remove duplicate collect()
        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getUserId());
        List<String> roles = userRoles.stream()
                .map(userRole -> {
                    Role role = roleRepository.findById(userRole.getRoleId())
                            .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));
                    return role.getRoleName().name();
                })
                .collect(Collectors.toList());

        String token = jwtUtil.generateToken(user, roles); // Pass roles to token generation
        sessionService.saveSession(username, token);
        return token;
    }

    public void logout(String username) {
        sessionService.removeSession(username);
    }

    public Map<String, Object> verifyToken(String token) {
        Claims claims = jwtUtil.validateToken(token);
        String username = claims.getSubject();
        if (!sessionService.isTokenValid(username, token)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return Map.of(
                "userId", claims.get("userId"),
                "username", username,
                "roles", claims.get("roles")
        );
    }

    public String refreshToken(String refreshToken) {
        String username = jwtUtil.getUsernameFromToken(refreshToken);
        String storedRefreshToken = redisTemplate.opsForValue().get("refresh:" + username);
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Get user roles for token generation
        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getUserId());
        List<String> roles = userRoles.stream()
                .map(userRole -> {
                    Role role = roleRepository.findById(userRole.getRoleId())
                            .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));
                    return role.getRoleName().name();
                })
                .collect(Collectors.toList());

        String newToken = jwtUtil.generateToken(user, roles);
        redisTemplate.opsForValue().set("session:" + username, newToken, 24, TimeUnit.HOURS);
        return newToken;
    }

    public void changePassword(ChangePasswordRequest request, String token) {
        String username = jwtUtil.getUsernameFromToken(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.NEW_PASSWORD_MUST_BE_DIFFERENT);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
        redisTemplate.delete("session:" + username);
        redisTemplate.delete("refresh:" + username);
    }

    public void sendResetPasswordEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));

        String resetToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("reset:" + user.getUsername(), resetToken, 10, TimeUnit.MINUTES);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Reset Password");
        message.setText("Click the link to reset your password: http://localhost:3000/reset-password?token=" + resetToken);
        mailSender.send(message);
    }

    public void resetPassword(String resetToken, String newPassword) {
        String username = redisTemplate.keys("reset:*").stream()
                .filter(key -> resetToken.equals(redisTemplate.opsForValue().get(key)))
                .map(key -> key.replace("reset:", ""))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_RESET_TOKEN));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
        redisTemplate.delete("reset:" + username);
        redisTemplate.delete("session:" + username);
        redisTemplate.delete("refresh:" + username);
    }

    public User getCurrentUser(String token) {
        String username = jwtUtil.getUsernameFromToken(token);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}