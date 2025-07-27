package com.web_game.common.Event;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InventoryEvent {
    private String action;
    private Long userId;
    private Long cardId;
    private Long inventoryId;
    private BigDecimal salePrice;
    private LocalDateTime timestamp;
}