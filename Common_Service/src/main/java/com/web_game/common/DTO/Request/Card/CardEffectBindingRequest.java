package com.web_game.common.DTO.Request.Card;

import com.web_game.common.Enum.TriggerType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CardEffectBindingRequest {

    @NotNull(message = "Effect ID is required")
    private Long effectId;

    @NotNull(message = "Trigger type is required")
    private TriggerType triggerType;
}
