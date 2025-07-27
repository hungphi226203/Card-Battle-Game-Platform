package com.web_game.common.DTO.shared;

import com.web_game.common.Entity.CardEffectBinding;
import com.web_game.common.Enum.CardType;
import com.web_game.common.Enum.Rarity;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CardDTO {
    private Long cardId;
    private String name;
    private CardType type;
    private Rarity rarity;
    private Integer mana;
    private Integer attack;
    private Integer health;
    private String description;
    private String animationId;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CardEffectBinding> effectBindings;
}