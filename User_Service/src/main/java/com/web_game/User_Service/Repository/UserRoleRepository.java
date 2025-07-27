package com.web_game.User_Service.Repository;

import com.web_game.common.Entity.UserRole;
import com.web_game.common.Enum.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    @Query("SELECT r.roleName FROM Role r " +
            "JOIN UserRole ur ON r.roleId = ur.roleId " +
            "WHERE ur.userId = :userId")
    List<String> findRoleNamesByUserId(@Param("userId") Long userId);

    // Check if user has specific role
    @Query("SELECT COUNT(ur) > 0 FROM UserRole ur " +
            "JOIN Role r ON ur.roleId = r.roleId " +
            "WHERE ur.userId = :userId AND r.roleName = :roleName")
    boolean existsByUserIdAndRoleName(@Param("userId") Long userId,
                                      @Param("roleName") RoleName roleName);

    // Find UserRole by userId and roleName
    @Query("SELECT ur FROM UserRole ur " +
            "JOIN Role r ON ur.roleId = r.roleId " +
            "WHERE ur.userId = :userId AND r.roleName = :roleName")
    Optional<UserRole> findByUserIdAndRoleName(@Param("userId") Long userId,
                                               @Param("roleName") RoleName roleName);

    // Delete role from user
    @Modifying
    @Transactional
    @Query("DELETE FROM UserRole ur WHERE ur.userId = :userId " +
            "AND ur.roleId = (SELECT r.roleId FROM Role r WHERE r.roleName = :roleName)")
    void deleteByUserIdAndRoleName(@Param("userId") Long userId,
                                   @Param("roleName") RoleName roleName);

    // Count admins
    @Query("SELECT COUNT(ur) FROM UserRole ur " +
            "JOIN Role r ON ur.roleId = r.roleId " +
            "WHERE r.roleName = 'ADMIN'")
    int countAdmins();

    // Find all UserRoles by userId
    List<UserRole> findByUserId(Long userId);
}