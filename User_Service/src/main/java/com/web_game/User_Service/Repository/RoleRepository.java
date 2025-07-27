package com.web_game.User_Service.Repository;

import com.web_game.common.Entity.Role;
import com.web_game.common.Enum.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByRoleName(RoleName roleName);
}