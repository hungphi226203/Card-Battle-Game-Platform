package com.web_game.common.DTO.Request.Transaction;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransactionRequest {
    @NotNull(message = "Inventory ID is required")
    @Min(value = 1, message = "Inventory ID must be positive")
    private Long inventoryId;
}