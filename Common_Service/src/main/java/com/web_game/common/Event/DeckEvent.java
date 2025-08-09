package com.web_game.common.Event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeckEvent {
    private String action;
    private Long userId;
    private List<Long> cardIds;
    private LocalDateTime timestamp;
}