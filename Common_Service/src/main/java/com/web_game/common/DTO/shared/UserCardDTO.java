package com.web_game.common.DTO.shared;

import com.web_game.common.Enum.Rarity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCardDTO {
    private Long inventoryId;
    private Long cardId;
    private Rarity rarity;
    private String cardName;
    private String image;
    private String mainImg;
    private int mana;
    private int attack;
    private int heal;
    private boolean isOnDeck;
    private boolean isForSale;
    private Float salePrice;
    private LocalDateTime acquiredAt;
}