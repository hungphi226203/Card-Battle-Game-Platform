package com.web_game.Authentication_Service.Repository;

import com.web_game.common.Entity.User;
import com.web_game.common.Entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findByUserId(Long userId);
    List<UserRole> findByRoleId(Long roleId);
    List<UserRole> findByUser(User user);
    void deleteByUserIdAndRoleId(Long userId, Long roleId);
    boolean existsByUserIdAndRoleId(Long userId, Long roleId);
}