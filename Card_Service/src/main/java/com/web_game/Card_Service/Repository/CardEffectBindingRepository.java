package com.web_game.Card_Service.Repository;

import com.web_game.common.Entity.CardEffectBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardEffectBindingRepository extends JpaRepository<CardEffectBinding, Long> {

    @Modifying
    @Query("DELETE FROM CardEffectBinding ceb WHERE ceb.card.cardId = :cardId")
    void deleteByCardId(@Param("cardId") Long cardId);

    @Query("SELECT ceb FROM CardEffectBinding ceb WHERE ceb.effect.effectId = :effectId")
    List<CardEffectBinding> findByEffectId(@Param("effectId") Long effectId);

    boolean existsByEffectEffectId(Long effectId);
}