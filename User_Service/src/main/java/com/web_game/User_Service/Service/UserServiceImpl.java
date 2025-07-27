package com.web_game.User_Service.Service;

import com.web_game.User_Service.Repository.RoleRepository;
import com.web_game.User_Service.Repository.UserRepository;
import com.web_game.User_Service.Repository.UserRoleRepository;
import com.web_game.common.Constant.RoleConstants;
import com.web_game.common.DTO.Request.User.UserUpdateRequest;
import com.web_game.common.DTO.shared.UserDTO;
import com.web_game.common.Entity.Role;
import com.web_game.common.Entity.User;
import com.web_game.common.Entity.UserRole;
import com.web_game.common.Enum.RoleName;
import com.web_game.common.Exception.AppException;
import com.web_game.common.Exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    public UserDTO getCurrentUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<String> roles = userRoleRepository.findRoleNamesByUserId(userId);
        return toDTO(user, roles);
    }

    @Transactional
    public UserDTO updateCurrentUserById(UserUpdateRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getImage() != null) user.setImage(request.getImage());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getDob() != null) user.setDob(request.getDob());

        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return getCurrentUserById(userId);
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> toDTO(user, userRoleRepository.findRoleNamesByUserId(user.getUserId())))
                .collect(Collectors.toList());
    }

    @Override
    public UserDTO getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return toDTO(user, userRoleRepository.findRoleNamesByUserId(userId));
    }

    @Override
    @Transactional
    public void lockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setLocked(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void unlockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setLocked(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void assignRole(Long userId, String roleName) {
        RoleName roleEnum;
        try {
            roleEnum = RoleName.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_ROLE);
        }

        userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Role role = roleRepository.findByRoleName(roleEnum)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_ROLE));

        if (roleName.equals(RoleConstants.ROLE_ADMIN) && userRoleRepository.countAdmins() > 0) {
            throw new AppException(ErrorCode.SINGLE_ADMIN_VIOLATION);
        }

        if (userRoleRepository.existsByUserIdAndRoleName(userId, roleEnum)) {
            return;
        }

        UserRole userRole = UserRole.builder()
                .userId(userId)
                .roleId(role.getRoleId())
                .assignedAt(LocalDateTime.now())
                .build();

        userRoleRepository.save(userRole);
    }

    @Override
    @Transactional
    public void removeRole(Long userId, String roleName) {
        RoleName roleEnum;
        try {
            roleEnum = RoleName.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_ROLE);
        }

        userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!userRoleRepository.existsByUserIdAndRoleName(userId, roleEnum)) {
            throw new AppException(ErrorCode.INVALID_ROLE);
        }

        userRoleRepository.deleteByUserIdAndRoleName(userId, roleEnum);
    }

    private UserDTO toDTO(User user, List<String> roles) {
        return UserDTO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .gender(user.getGender())
                .dob(user.getDob())
                .balance(user.getBalance())
                .image(user.getImage())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .locked(user.isLocked())
                .roles(roles)
                .build();
    }
}