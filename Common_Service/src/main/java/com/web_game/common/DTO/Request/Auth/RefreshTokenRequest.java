package com.web_game.common.DTO.Request.Auth;

import com.web_game.common.Enum.DeviceType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token không được để trống")
    private String refreshToken;

    private String deviceType = "web"; // For consistency

    public DeviceType getDeviceTypeEnum() {
        return DeviceType.fromString(this.deviceType);
    }
}