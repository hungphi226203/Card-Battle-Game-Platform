package com.web_game.common.DTO.Request.User;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleAssignmentRequest {
    @NotBlank(message = "Role name is required")
    private String roleName;
}