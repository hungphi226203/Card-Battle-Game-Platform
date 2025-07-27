package com.web_game.common.DTO.shared;

import com.web_game.common.Enum.CardType;
import com.web_game.common.Enum.Rarity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CollectionCardDTO {
    private Long cardId;
    private String name;
    private CardType type;
    private Rarity rarity;
    private int mana;
    private Integer attack;
    private Integer health;
    private String image;
    private String description;
    private boolean owned;
    private int quantity;
}