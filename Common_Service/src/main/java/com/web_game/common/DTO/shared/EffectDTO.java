package com.web_game.common.DTO.shared;

import com.web_game.common.Enum.EffectType;
import com.web_game.common.Enum.Target;
import lombok.Data;

@Data
public class EffectDTO {
    private Long effectId;
    private EffectType type;
    private Integer value;
    private Target target;
    private String animationId;
    private String buffType;
    private Integer duration;
    private Boolean isStartOfTurn;
    private String summonMinionIds;
}