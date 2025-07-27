package com.web_game.Inventory_Service.Service;

import com.web_game.common.DTO.Request.Deck.UpdateDeckRequest;
import com.web_game.common.DTO.Respone.CollectionResponse;
import com.web_game.common.DTO.Respone.DeckResponse;

public interface DeckService {
    DeckResponse getUserDeck(Long userId);
    DeckResponse updateDeck(Long userId, UpdateDeckRequest request);
    DeckResponse addCardToDeck(Long userId, Long inventoryId);
    DeckResponse removeCardFromDeck(Long userId, Long inventoryId);
    CollectionResponse getUserCollection(Long userId);
}