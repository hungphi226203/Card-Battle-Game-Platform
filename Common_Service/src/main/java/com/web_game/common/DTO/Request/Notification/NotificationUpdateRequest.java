package com.web_game.common.DTO.Request.Notification;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationUpdateRequest {
    private String groupId; // ID của nhóm thông báo
    private String message;
    private String type;
}
