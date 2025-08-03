package com.web_game.common.DTO.Request.Card;

import com.web_game.common.Enum.CardType;
import com.web_game.common.Enum.Rarity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CardCreateRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be less than 100 characters")
    private String name;

    @NotNull(message = "Card type is required")
    private CardType type;

    @NotNull(message = "Rarity is required")
    private Rarity rarity;

    @NotNull(message = "Mana is required")
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