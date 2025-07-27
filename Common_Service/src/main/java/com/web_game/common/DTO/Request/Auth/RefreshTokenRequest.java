package com.web_game.common.DTO.Request.Auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest {
    @NotBlank(message = "INVALID_KEY")
    private String refreshToken;
}