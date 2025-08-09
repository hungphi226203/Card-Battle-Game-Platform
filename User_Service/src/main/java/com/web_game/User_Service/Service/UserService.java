package com.web_game.User_Service.Service;

import com.web_game.common.DTO.Request.User.UserUpdateRequest;
import com.web_game.common.DTO.shared.UserDTO;

import java.util.List;

public interface UserService {

    public UserDTO getCurrentUserById(Long userId);

    public UserDTO updateCurrentUserById(UserUpdateRequest request, Long userId);

    List<UserDTO> getAllUsers();

    UserDTO getUser(Long userId);

    void lockUser(Long userId);

    void unlockUser(Long userId);

    void assignRole(Long userId, String roleName);

    void removeRole(Long userId, String roleName);

    void updateStageOnly(Long userId, int stage);
}