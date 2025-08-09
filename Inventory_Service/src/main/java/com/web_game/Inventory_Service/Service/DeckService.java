package com.web_game.Inventory_Service.Service;

import com.web_game.common.DTO.Request.Deck.UpdateDeckRequest;
import com.web_game.common.DTO.Respone.CollectionResponse;
import com.web_game.common.DTO.Respone.DeckResponse;

public interface DeckService {
    DeckResponse getUserDeck(Long userId);
    DeckResponse updateDeck(Long userId, UpdateDeckRequest request);
    CollectionResponse getUserCollection(Long userId);
    DeckResponse getOpponentDeck(Long opponentId);
}