package com.web_game.common.DTO.Request.Auth;

import com.web_game.common.Enum.DeviceType;
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

    private String deviceId; // Optional: unique device identifier

    private String deviceType = "web"; // Default to web

    public DeviceType getDeviceTypeEnum() {
        return DeviceType.fromString(this.deviceType);
    }
}