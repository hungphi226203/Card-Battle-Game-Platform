package com.web_game.common.DTO.Respone;

import com.web_game.common.Enum.Rarity;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GachaHistoryResponse {
    private Long id;
    private Long userId;
    private Long cardId;
    private String cardName;
    private Rarity rarity;
    private LocalDateTime timestamp;
}