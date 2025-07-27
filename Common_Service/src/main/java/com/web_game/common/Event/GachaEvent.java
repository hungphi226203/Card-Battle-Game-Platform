package com.web_game.common.Event;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GachaEvent {
    private Long userId;
    private Long cardId;
    private LocalDateTime timestamp;
}