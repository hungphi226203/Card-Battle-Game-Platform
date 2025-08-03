package com.web_game.common.DTO.Request.Card;

import com.web_game.common.Enum.CardType;
import com.web_game.common.Enum.Rarity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CardUpdateRequest {

    @Size(max = 100, message = "Name must be less than 100 characters")
    private String name;

    private CardType type;

    private Rarity rarity;

    @Min(value = 0, message = "Mana must be non-negative")
    private Integer mana;

    @Min(value = 0, message = "Attack must be non-negative")
    private Integer attack;

    @Min(value = 0, message = "Health must be non-negative")
    private Integer health;

    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;

    @Size(max = 255, message = "Image URL must be less than 255 characters")
    private String imageUrl;

    @Size(max = 255, message = "Image URL must be less than 255 characters")
    private String mainImg;

    @Valid
    private List<CardEffectBindingRequest> effectBindings;
}