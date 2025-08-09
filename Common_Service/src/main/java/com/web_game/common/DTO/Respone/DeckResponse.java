package com.web_game.common.DTO.Respone;

import com.web_game.common.DTO.shared.DeckCardDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeckResponse {
    private Long userId;
    private List<DeckCardDTO> cards;
    private int totalCards;
}