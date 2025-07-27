package com.web_game.common.DTO.Request.UserCard;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SellCardRequest {
    @NotNull(message = "Sale price is required")
    @Min(value = 1, message = "Sale price must be greater than 0")
    private BigDecimal salePrice;
}