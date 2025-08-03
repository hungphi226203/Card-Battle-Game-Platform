package com.web_game.common.DTO.Respone;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InventoryResponse {
    private Long inventoryId;
    private Long userId;
    private Long cardId;
    private Boolean isForSale;
    private BigDecimal salePrice;
    private LocalDateTime acquiredAt;
}