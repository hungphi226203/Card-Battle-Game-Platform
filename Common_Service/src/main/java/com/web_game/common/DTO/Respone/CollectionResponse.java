package com.web_game.common.DTO.Respone;

import com.web_game.common.DTO.shared.CollectionCardDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CollectionResponse {
    private Long userId;
    private String username;
    private List<CollectionCardDTO> cards;
    private int totalCards;
    private int ownedCards;
    private double completionPercentage;
}