package com.web_game.common.DTO.shared;

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
    private String cardName;
    private String image;
    private boolean isOnDeck;
    private boolean isForSale;
    private BigDecimal salePrice;
    private LocalDateTime acquiredAt;
}