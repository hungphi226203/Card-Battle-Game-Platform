package com.web_game.common.DTO.Request.Auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthRequest {
    @NotBlank(message = "INVALID_KEY")
    private String username;
    @NotBlank(message = "INVALID_PASSWORD")
    @Size(min = 6, message = "INVALID_PASSWORD_LENGTH")
    private String password;
}