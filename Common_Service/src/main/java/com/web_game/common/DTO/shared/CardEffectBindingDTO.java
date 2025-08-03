package com.web_game.common.DTO.shared;

import com.web_game.common.Enum.TriggerType;
import lombok.Data;

@Data
public class CardEffectBindingDTO {
    private Long bindingId;
    private TriggerType triggerType;
    private EffectDTO effect;
}