package com.web_game.Event_Service.Repository;

import com.web_game.common.Entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {
}
