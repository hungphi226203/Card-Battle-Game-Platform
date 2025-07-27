package com.web_game.common.DTO.Request.Auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class LoginRequest {
    @NotBlank(message = "USERNAME_INVALID")
    private String username;

    @NotBlank(message = "INVALID_PASSWORD")
    @Size(min = 6, message = "INVALID_PASSWORD_LENGTH")
    private String password;
}