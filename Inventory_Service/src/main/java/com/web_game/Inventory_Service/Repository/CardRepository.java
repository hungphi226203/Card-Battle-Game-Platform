package com.web_game.Inventory_Service.Repository;

import com.web_game.common.Entity.Card;
import com.web_game.common.Enum.Rarity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    Optional<Card> findByCardId(Long cardId);
    List<Card> findByRarity(Rarity rarity);
}