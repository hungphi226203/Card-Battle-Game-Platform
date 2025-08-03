package com.web_game.common.DTO.Request.Deck;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddCardToDeckRequest {
    @NotNull(message = "Inventory ID không được null")
    private Long inventoryId;
    //gửi dạng lisst listinventopryid
}