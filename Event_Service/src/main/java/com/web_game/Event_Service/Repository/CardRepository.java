package com.web_game.Event_Service.Repository;

import com.web_game.common.Entity.Card;
import com.web_game.common.Enum.Rarity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByRarity(Rarity rarity);
}
