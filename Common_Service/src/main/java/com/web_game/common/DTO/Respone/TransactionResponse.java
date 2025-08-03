package com.web_game.common.DTO.Respone;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionResponse {
    private Long transactionId;
    private Long sellerId;
    private Long buyerId;
    private Long inventoryId;
    private Float price;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}