package com.web_game.common.DTO.Request.UserCard;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserCardCreateRequest {
    @NotNull(message = "Card ID is required")
    @Min(value = 1, message = "Card ID must be positive")
    private Long cardId;
}