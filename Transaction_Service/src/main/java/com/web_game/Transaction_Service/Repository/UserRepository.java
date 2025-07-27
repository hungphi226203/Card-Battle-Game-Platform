package com.web_game.Transaction_Service.Repository;

import com.web_game.common.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
