package com.web_game.common.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.web_game.common.Enum.EffectType;
import com.web_game.common.Enum.Target;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "effects")
public class CardEffect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "effect_id")
    private Long effectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EffectType type;

    private Integer value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Target target;

    @Column(name = "animation_id")
    private String animationId;

    @Column(name = "buff_type")
    private String buffType;

    private Integer duration;

    @Column(name = "is_start_of_turn")
    private Boolean isStartOfTurn;

    @Column(name = "summon_minion_ids", columnDefinition = "TEXT")
    private String summonMinionIds;

    @OneToMany(mappedBy = "effect")
    @JsonIgnore
    private List<CardEffectBinding> effectBindings;
}