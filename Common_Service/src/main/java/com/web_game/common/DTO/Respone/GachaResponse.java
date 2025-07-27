package com.web_game.common.DTO.Respone;

import lombok.Data;

@Data
public class GachaResponse {
    private Long id;
    private Long userId;
    private Long cardId;
    private String cardName;
}