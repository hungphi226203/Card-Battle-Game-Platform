package com.web_game.common.DTO.shared;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoleAssignmentInfo {
    private String roleName;
    private LocalDateTime assignedAt;
}