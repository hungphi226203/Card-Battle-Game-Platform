package com.web_game.Card_Service.Repository;

import com.web_game.common.Entity.CardEffect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardEffectRepository extends JpaRepository<CardEffect, Long> {
}