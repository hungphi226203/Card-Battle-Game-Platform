package com.web_game.Inventory_Service.Service;

import com.web_game.common.DTO.Request.UserCard.SellCardRequest;
import com.web_game.common.DTO.Respone.InventoryResponse;
import com.web_game.common.DTO.shared.UserCardDTO;

import java.util.List;

public interface InventoryService {
    List<UserCardDTO> getInventory(Long userId);

    UserCardDTO getCardInInventory(Long userId, Long cardId);

    void addCardToInventory(Long userId, Long cardId, String addedBy);

    void removeCardFromInventory(Long userId, Long cardId, String removedBy);

    void listCardForSale(Long inventoryId, SellCardRequest request, Long userId);

    void cancelCardSale(Long inventoryId, Long userId);

    List<InventoryResponse> getCardsForSale();

    public List<InventoryResponse> getMyCardsForSale(Long userId);

    public UserCardDTO getCardByInventoryId(Long inventoryId);
}