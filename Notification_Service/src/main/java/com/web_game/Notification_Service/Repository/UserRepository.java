package com.web_game.Notification_Service.Repository;

import com.web_game.common.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u.userId FROM User u")
    List<Long> findAllUserIds();
}

