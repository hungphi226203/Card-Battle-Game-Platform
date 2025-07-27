package com.web_game.Card_Service.Service;

import com.web_game.common.DTO.Request.Card.*;
import com.web_game.common.DTO.shared.CardDTO;
import com.web_game.common.DTO.shared.EffectDTO;
import com.web_game.common.Entity.Card;

import java.util.List;

public interface CardService {
    public List<CardDTO> getAllCards();

    public CardDTO getCard(Long cardId);

    public CardDTO createCard(CardCreateRequest request);

    public CardDTO updateCard(Long cardId, CardUpdateRequest request);

    public void deleteCard(Long cardId);

    public void createEffectBindings(Card card, List<CardEffectBindingRequest> bindingRequests);

    List<EffectDTO> getAllEffects();

    EffectDTO getEffect(Long effectId);

    EffectDTO createEffect(EffectCreateRequest request);

    EffectDTO updateEffect(Long effectId, EffectUpdateRequest request);

    void deleteEffect(Long effectId);
}