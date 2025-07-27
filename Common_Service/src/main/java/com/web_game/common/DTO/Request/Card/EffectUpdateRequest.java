package com.web_game.common.DTO.Request.Card;

import com.web_game.common.Enum.EffectType;
import com.web_game.common.Enum.Target;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EffectUpdateRequest {

    @Size(max = 100, message = "Effect name must be less than 100 characters")
    private String name;

    private EffectType type;

    private Integer value;

    private Target target;

    @Size(max = 100, message = "Animation ID must be less than 100 characters")
    private String animationId;

    @Size(max = 50, message = "Buff type must be less than 50 characters")
    private String buffType;

    private Integer duration;

    private Boolean isStartOfTurn;

    @Size(max = 500, message = "Summon minion IDs must be less than 500 characters")
    private String summonMinionIds;
}