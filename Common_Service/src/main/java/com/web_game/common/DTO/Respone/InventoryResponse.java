package com.web_game.common.DTO.Respone;

import com.web_game.common.Enum.CardType;
import com.web_game.common.Enum.Rarity;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InventoryResponse {
    private Long inventoryId;
    private Long userId;
    private Long cardId;
    private Boolean isForSale;
    private Float salePrice;
    private LocalDateTime acquiredAt;

    private String cardName;
    private String imageUrl;
    private String mainImg;
    private Rarity rarity;
    private CardType type;

    private String sellerName;
}