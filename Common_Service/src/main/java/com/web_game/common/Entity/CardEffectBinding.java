package com.web_game.common.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.web_game.common.Enum.TriggerType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "card_effect_bindings")
public class CardEffectBinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "binding_id")
    private Long bindingId;

    @ManyToOne
    @JoinColumn(name = "card_id", nullable = false)
    @JsonIgnore
    private Card card;

    @ManyToOne
    @JoinColumn(name = "effect_id", nullable = false)
    private CardEffect effect;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private TriggerType triggerType;
}