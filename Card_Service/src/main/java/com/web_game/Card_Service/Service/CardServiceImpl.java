package com.web_game.Card_Service.Service;

import com.web_game.Card_Service.Repository.CardEffectBindingRepository;
import com.web_game.Card_Service.Repository.CardEffectRepository;
import com.web_game.Card_Service.Repository.CardRepository;
import com.web_game.common.DTO.Request.Card.*;
import com.web_game.common.DTO.shared.CardDTO;
import com.web_game.common.DTO.shared.EffectDTO;
import com.web_game.common.Entity.Card;
import com.web_game.common.Entity.CardEffect;
import com.web_game.common.Entity.CardEffectBinding;
import com.web_game.common.Exception.AppException;
import com.web_game.common.Exception.ErrorCode;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CardServiceImpl implements CardService{
    @Autowired
    CardRepository cardRepository;

    @Autowired
    CardEffectRepository cardEffectRepository;

    @Autowired
    CardEffectBindingRepository cardEffectBindingRepository;

    public List<CardDTO> getAllCards() {
        return cardRepository.findAll().stream()
                .map(card -> {
                    CardDTO cardDTO = new CardDTO();
                    BeanUtils.copyProperties(card, cardDTO);
                    return cardDTO;
                })
                .collect(Collectors.toList());
    }

    public CardDTO getCard(Long cardId) {
        Card card = cardRepository.findByCardId(cardId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CARD_VALUE));
        CardDTO cardDTO = new CardDTO();
        BeanUtils.copyProperties(card, cardDTO);
        return cardDTO;
    }

    @Override
    @Transactional
    public CardDTO createCard(CardCreateRequest request) {
        Card card = new Card();
        BeanUtils.copyProperties(request, card);
        card.setCreatedAt(LocalDateTime.now());
        card.setUpdatedAt(LocalDateTime.now());

        // Lưu card trước
        Card savedCard = cardRepository.save(card);

        // Tạo các effect bindings nếu có
        if (request.getEffectBindings() != null && !request.getEffectBindings().isEmpty()) {
            createEffectBindings(savedCard, request.getEffectBindings());
        }

        CardDTO dto = new CardDTO();
        BeanUtils.copyProperties(savedCard, dto);
        return dto;
    }

    @Override
    @Transactional
    public CardDTO updateCard(Long cardId, CardUpdateRequest request) {
        Card card = cardRepository.findByCardId(cardId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CARD_VALUE));

        // Copy các field cơ bản
        if (request.getName() != null) card.setName(request.getName());
        if (request.getType() != null) card.setType(request.getType());
        if (request.getRarity() != null) card.setRarity(request.getRarity());
        if (request.getMana() != null) card.setMana(request.getMana());
        if (request.getAttack() != null) card.setAttack(request.getAttack());
        if (request.getHealth() != null) card.setHealth(request.getHealth());
        if (request.getDescription() != null) card.setDescription(request.getDescription());

        card.setUpdatedAt(LocalDateTime.now());
        Card savedCard = cardRepository.save(card);

        // Cập nhật effect bindings nếu có
        if (request.getEffectBindings() != null) {
            // Xóa tất cả binding cũ
            cardEffectBindingRepository.deleteByCardId(cardId);

            // Tạo binding mới
            if (!request.getEffectBindings().isEmpty()) {
                createEffectBindings(savedCard, request.getEffectBindings());
            }
        }

        CardDTO dto = new CardDTO();
        BeanUtils.copyProperties(savedCard, dto);
        return dto;
    }

    @Override
    @Transactional
    public void deleteCard(Long cardId) {
        Card card = cardRepository.findByCardId(cardId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CARD_VALUE));

        // Xóa tất cả effect bindings trước
        cardEffectBindingRepository.deleteByCardId(cardId);

        // Xóa card
        cardRepository.delete(card);
    }

    public void createEffectBindings(Card card, List<CardEffectBindingRequest> bindingRequests) {
        for (CardEffectBindingRequest bindingRequest : bindingRequests) {
            // Kiểm tra effect có tồn tại không
            CardEffect effect = cardEffectRepository.findById(bindingRequest.getEffectId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_EFFECT_VALUE));

            // Tạo binding
            CardEffectBinding binding = new CardEffectBinding();
            binding.setCard(card);
            binding.setEffect(effect);
            binding.setTriggerType(bindingRequest.getTriggerType());

            cardEffectBindingRepository.save(binding);
        }
    }

    @Override
    public List<EffectDTO> getAllEffects() {
        return cardEffectRepository.findAll().stream()
                .map(effect -> {
                    EffectDTO effectDTO = new EffectDTO();
                    BeanUtils.copyProperties(effect, effectDTO);
                    return effectDTO;
                })
                .collect(Collectors.toList());
    }

    @Override
    public EffectDTO getEffect(Long effectId) {
        CardEffect effect = cardEffectRepository.findById(effectId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_EFFECT_VALUE));

        EffectDTO effectDTO = new EffectDTO();
        BeanUtils.copyProperties(effect, effectDTO);
        return effectDTO;
    }

    @Override
    public EffectDTO createEffect(EffectCreateRequest request) {
        CardEffect effect = new CardEffect();
        BeanUtils.copyProperties(request, effect);

        CardEffect savedEffect = cardEffectRepository.save(effect);

        EffectDTO dto = new EffectDTO();
        BeanUtils.copyProperties(savedEffect, dto);
        return dto;
    }

    @Override
    public EffectDTO updateEffect(Long effectId, EffectUpdateRequest request) {
        CardEffect effect = cardEffectRepository.findById(effectId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_EFFECT_VALUE));

        if (request.getType() != null) effect.setType(request.getType());
        if (request.getValue() != null) effect.setValue(request.getValue());
        if (request.getTarget() != null) effect.setTarget(request.getTarget());
        if (request.getAnimationId() != null) effect.setAnimationId(request.getAnimationId());
        if (request.getBuffType() != null) effect.setBuffType(request.getBuffType());
        if (request.getDuration() != null) effect.setDuration(request.getDuration());
        if (request.getIsStartOfTurn() != null) effect.setIsStartOfTurn(request.getIsStartOfTurn());
        if (request.getSummonMinionIds() != null) effect.setSummonMinionIds(request.getSummonMinionIds());

        CardEffect savedEffect = cardEffectRepository.save(effect);

        EffectDTO dto = new EffectDTO();
        BeanUtils.copyProperties(savedEffect, dto);
        return dto;
    }

    @Override
    @Transactional
    public void deleteEffect(Long effectId) {
        CardEffect effect = cardEffectRepository.findById(effectId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_EFFECT_VALUE));

        if (!effect.getEffectBindings().isEmpty()) {
            throw new AppException(ErrorCode.EFFECT_IN_USE);
        }

        cardEffectRepository.delete(effect);
    }
}