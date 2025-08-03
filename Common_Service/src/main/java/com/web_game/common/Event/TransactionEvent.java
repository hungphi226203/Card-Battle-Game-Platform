package com.web_game.common.Event;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionEvent {
    private String action;
    private Long transactionId;
    private Long sellerId;
    private Long buyerId;
    private Long inventoryId;
    private Float price;
    private LocalDateTime timestamp;
}