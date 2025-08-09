package com.web_game.Authentication_Service.Service.ServiceImpl;

import com.web_game.Authentication_Service.Repository.RoleRepository;
import com.web_game.Authentication_Service.Repository.UserRepository;
import com.web_game.Authentication_Service.Repository.UserRoleRepository;
import com.web_game.Authentication_Service.Service.AuthService;
import com.web_game.Authentication_Service.Service.SessionService;
import com.web_game.common.DTO.Request.Auth.AuthRequest;
import com.web_game.common.DTO.Request.Auth.ChangePasswordRequest;
import com.web_game.common.DTO.Request.Auth.RegisterRequest;
import com.web_game.common.Entity.Role;
import com.web_game.common.Entity.User;
import com.web_game.common.Entity.UserRole;
import com.web_game.common.Enum.DeviceType;
import com.web_game.common.Enum.RoleName;
import com.web_game.common.Event.UserRegisteredEvent;
import com.web_game.common.Exception.AppException;
import com.web_game.common.Exception.ErrorCode;
import com.web_game.Authentication_Service.Util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SessionService sessionService;
    private final RedisTemplate<String, String> redisTemplate;
    private final JavaMailSender mailSender;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Override
    public User register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
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
            log.error("Failed to publish UserRegistered event: {}", e.getMessage());
        }

        log.info("User registered successfully: {}", request.getUsername());
        return user;
    }

    @Override
    public String login(AuthRequest request) {
        log.info("Login attempt for user: {} on device: {}", request.getUsername(), request.getDeviceType());

        DeviceType deviceType = request.getDeviceTypeEnum();

        try {
            log.info("Authenticating user: {}", request.getUsername());
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            log.info("Authentication successful for user: {}", request.getUsername());

            String existingToken = sessionService.getSession(request.getUsername(), deviceType);
            if (existingToken != null && !jwtUtil.isTokenExpired(existingToken)) {
                log.info("Returning existing valid token for user: {} on device: {} after successful authentication",
                        request.getUsername(), deviceType.getValue());
                return existingToken;
            }

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

            List<UserRole> userRoles = userRoleRepository.findByUserId(user.getUserId());
            List<String> roles = userRoles.stream()
                    .map(userRole -> {
                        Role role = roleRepository.findById(userRole.getRoleId())
                                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));
                        return role.getRoleName().name();
                    })
                    .collect(Collectors.toList());

            String token = jwtUtil.generateToken(user, roles, deviceType, request.getDeviceId());

            sessionService.storeSession(request.getUsername(), deviceType, token, expiration);

            log.info("User {} logged in successfully on device: {}", request.getUsername(), deviceType.getValue());
            return token;

        } catch (AuthenticationException e) {
            log.error("Authentication failed for user: {} - {}", request.getUsername(), e.getMessage());
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        } catch (Exception e) {
            log.error("Login error for user: {} - {}", request.getUsername(), e.getMessage(), e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    @Override
    public void logout(String token) {
        try {
            String username = jwtUtil.getUsernameFromToken(token);
            DeviceType deviceType = jwtUtil.getDeviceTypeFromToken(token);

            sessionService.removeSession(username, deviceType);
            log.info("User {} logged out from device: {}", username, deviceType.getValue());
        } catch (Exception e) {
            log.error("Error during logout", e);
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
    }

    @Override
    public String refreshToken(String refreshToken) {
        if (jwtUtil.isTokenExpired(refreshToken)) {
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String username = jwtUtil.getUsernameFromToken(refreshToken);
        DeviceType deviceType = jwtUtil.getDeviceTypeFromToken(refreshToken);

        // Verify the refresh token is still valid in session
        if (!sessionService.isTokenValid(username, deviceType, refreshToken)) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        List<String> roles = jwtUtil.getRolesFromToken(refreshToken);
        String deviceId = jwtUtil.getDeviceIdFromToken(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Generate new token
        String newToken = jwtUtil.generateToken(user, roles, deviceType, deviceId);

        // Update session
        sessionService.storeSession(username, deviceType, newToken, expiration);

        return newToken;
    }

    @Override
    public Map<String, Object> verifyToken(String token) {
        try {
            Claims claims = jwtUtil.validateToken(token);
            String username = claims.getSubject();
            DeviceType deviceType = DeviceType.fromString((String) claims.get("deviceType"));

            // Check if token is still valid in session
            if (!sessionService.isTokenValid(username, deviceType, token)) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            return Map.of(
                    "userId", claims.get("userId"),
                    "username", username,
                    "roles", claims.get("roles"),
                    "deviceType", deviceType.getValue()
            );
        } catch (Exception e) {
            log.error("Token verification failed", e);
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
    }

    @Override
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
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Remove ALL sessions for this user (force re-login on all devices)
        sessionService.removeAllSessions(username);

        log.info("Password changed for user: {}, all sessions invalidated", username);
    }

    @Override
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

    @Override
    public void resetPassword(String resetToken, String newPassword) {
        String username = redisTemplate.keys("reset:*").stream()
                .filter(key -> resetToken.equals(redisTemplate.opsForValue().get(key)))
                .map(key -> key.replace("reset:", ""))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_RESET_TOKEN));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        redisTemplate.delete("reset:" + username);
        sessionService.removeAllSessions(username);

        log.info("Password reset for user: {}, all sessions invalidated", username);
    }
}