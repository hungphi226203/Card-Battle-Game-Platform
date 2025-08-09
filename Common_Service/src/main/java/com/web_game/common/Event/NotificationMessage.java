package com.web_game.common.Event;

import lombok.Data;

@Data
public class NotificationMessage {
    private Long userId;
    private String type;
    private String  message;
    private Long timestamp;
}