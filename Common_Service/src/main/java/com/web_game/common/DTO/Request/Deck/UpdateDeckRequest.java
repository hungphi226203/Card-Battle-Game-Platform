package com.web_game.common.DTO.Request.Deck;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDeckRequest {
    @NotEmpty(message = "Danh sách thẻ không được trống")
    @Size(min = 30, max = 30, message = "Deck phải có đúng 30 thẻ")
    private List<Long> inventoryIds;
}