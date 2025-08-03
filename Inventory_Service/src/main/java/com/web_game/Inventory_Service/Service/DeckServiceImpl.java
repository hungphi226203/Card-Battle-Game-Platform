package com.web_game.Inventory_Service.Service;

import com.web_game.Inventory_Service.Event.DeckEvent;
import com.web_game.Inventory_Service.Repository.CardRepository;
import com.web_game.Inventory_Service.Repository.UserCardRepository;
import com.web_game.common.DTO.Request.Deck.UpdateDeckRequest;
import com.web_game.common.DTO.Respone.CollectionResponse;
import com.web_game.common.DTO.Respone.DeckResponse;
import com.web_game.common.DTO.shared.CollectionCardDTO;
import com.web_game.common.DTO.shared.DeckCardDTO;
import com.web_game.common.DTO.shared.EffectDTO;
import com.web_game.common.Entity.Card;
import com.web_game.common.Entity.Inventory;
import com.web_game.common.Exception.AppException;
import com.web_game.common.Exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DeckServiceImpl implements DeckService {

    @Autowired
    private UserCardRepository userCardRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private KafkaTemplate<String, DeckEvent> deckKafkaTemplate;

    @Override
    public DeckResponse getUserDeck(Long userId) {
        List<Inventory> deckCards = userCardRepository.findDeckCardsByUserId(userId);

        if (deckCards.size() != 30) {
            throw new AppException(ErrorCode.INVALID_DECK_SIZE);
        }

        List<DeckCardDTO> deckCardDTOs = deckCards.stream()
                .map(this::convertToDeckCardDTO)
                .collect(Collectors.toList());

        DeckResponse response = new DeckResponse();
        response.setUserId(userId);
        response.setCards(deckCardDTOs);
        response.setTotalCards(deckCardDTOs.size());
        return response;
    }

    @Override
    @Transactional
    public DeckResponse updateDeck(Long userId, UpdateDeckRequest request) {
        List<Inventory> requestedCards = userCardRepository.findByInventoryIdInAndUserId(
                request.getInventoryIds(), userId);

        if (requestedCards.size() != 30) {
            throw new AppException(ErrorCode.INVALID_CARD_OWNERSHIP);
        }

        boolean hasCardForSale = requestedCards.stream().anyMatch(Inventory::getIsForSale);
        if (hasCardForSale) {
            throw new AppException(ErrorCode.CARD_IN_SALE_CANNOT_BE_IN_DECK);
        }

        List<Inventory> oldDeckCards = userCardRepository.findDeckCardsByUserId(userId);
        oldDeckCards.forEach(card -> card.setIsOnDeck(false));
        userCardRepository.saveAll(oldDeckCards);

        requestedCards.forEach(card -> card.setIsOnDeck(true));
        userCardRepository.saveAll(requestedCards);

        sendDeckEventAsync("UPDATE_DECK", userId,
                requestedCards.stream().map(Inventory::getCardId).collect(Collectors.toList()));

        return getUserDeck(userId);
    }

    @Override
    @Transactional
    public DeckResponse addCardToDeck(Long userId, Long inventoryId) {
        List<Inventory> currentDeck = userCardRepository.findDeckCardsByUserId(userId);
        if (currentDeck.size() >= 30) throw new AppException(ErrorCode.DECK_FULL);

        Inventory inventory = userCardRepository.findByInventoryIdAndUserId(inventoryId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_CARD_NOT_FOUND));

        if (inventory.getIsOnDeck()) throw new AppException(ErrorCode.CARD_ALREADY_IN_DECK);
        if (inventory.getIsForSale()) throw new AppException(ErrorCode.CARD_IN_SALE_CANNOT_BE_IN_DECK);

        inventory.setIsOnDeck(true);
        userCardRepository.save(inventory);

        sendDeckEventAsync("ADD_CARD_TO_DECK", userId, List.of(inventory.getCardId()));
        return getUserDeck(userId);
    }

    @Override
    @Transactional
    public DeckResponse removeCardFromDeck(Long userId, Long inventoryId) {
        Inventory inventory = userCardRepository.findByInventoryIdAndUserId(inventoryId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_CARD_NOT_FOUND));

        if (!inventory.getIsOnDeck()) throw new AppException(ErrorCode.CARD_NOT_IN_DECK);

        inventory.setIsOnDeck(false);
        userCardRepository.save(inventory);

        sendDeckEventAsync("REMOVE_CARD_FROM_DECK", userId, List.of(inventory.getCardId()));
        return getUserDeck(userId);
    }

    @Override
    public CollectionResponse getUserCollection(Long userId) {
        List<Card> allCards = cardRepository.findAll();
        List<Inventory> userCards = userCardRepository.findByUserId(userId);

        Map<Long, Long> cardCountMap = userCards.stream()
                .collect(Collectors.groupingBy(Inventory::getCardId, Collectors.counting()));

        List<CollectionCardDTO> collectionCards = allCards.stream().map(card -> {
            CollectionCardDTO dto = new CollectionCardDTO();
            dto.setCardId(card.getCardId());
            dto.setName(card.getName());
            dto.setType(card.getType());
            dto.setRarity(card.getRarity());
            dto.setMana(card.getMana());
            dto.setAttack(card.getAttack());
            dto.setHealth(card.getHealth());
            dto.setImage(card.getMainImg());
            Long count = cardCountMap.getOrDefault(card.getCardId(), 0L);
            dto.setOwned(count > 0);
            dto.setQuantity(count.intValue());
            return dto;
        }).collect(Collectors.toList());

        int ownedCards = (int) collectionCards.stream().filter(CollectionCardDTO::isOwned).count();
        double completionPercentage = allCards.isEmpty() ? 0 :
                (double) ownedCards / allCards.size() * 100;

        CollectionResponse response = new CollectionResponse();
        response.setUserId(userId);
        response.setCards(collectionCards);
        response.setTotalCards(allCards.size());
        response.setOwnedCards(ownedCards);
        response.setCompletionPercentage(Math.round(completionPercentage * 100.0) / 100.0);
        return response;
    }

    @Async
    public void sendDeckEventAsync(String action, Long userId, List<Long> cardIds) {
        DeckEvent event = new DeckEvent();
        event.setAction(action);
        event.setUserId(userId);
        event.setCardIds(cardIds);
        event.setTimestamp(LocalDateTime.now());
        deckKafkaTemplate.send("deck-events", event);
    }

    private DeckCardDTO convertToDeckCardDTO(Inventory inventory) {
        Card card = cardRepository.findById(inventory.getCardId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CARD_VALUE));

        DeckCardDTO dto = new DeckCardDTO();
        dto.setInventoryId(inventory.getInventoryId());
        dto.setCardId(card.getCardId());
        dto.setName(card.getName());
        dto.setType(card.getType());
        dto.setRarity(card.getRarity());
        dto.setMana(card.getMana());
        dto.setAttack(card.getAttack());
        dto.setHealth(card.getHealth());
        dto.setImage(card.getImageUrl());
        dto.setMainImg(card.getMainImg());

        if (card.getEffectBindings() != null) {
            dto.setEffects(card.getEffectBindings().stream().map(binding -> {
                EffectDTO effectDTO = new EffectDTO();
                effectDTO.setEffectId(binding.getEffect().getEffectId());
                effectDTO.setType(binding.getEffect().getType());
                effectDTO.setValue(binding.getEffect().getValue());
                effectDTO.setTarget(binding.getEffect().getTarget());
                effectDTO.setAnimationId(binding.getEffect().getAnimationId());
                effectDTO.setBuffType(binding.getEffect().getBuffType());
                effectDTO.setDuration(binding.getEffect().getDuration());
                effectDTO.setIsStartOfTurn(binding.getEffect().getIsStartOfTurn());
                effectDTO.setSummonMinionIds(binding.getEffect().getSummonMinionIds());
                effectDTO.setTriggerType(binding.getTriggerType());
                return effectDTO;
            }).collect(Collectors.toList()));
        }

        return dto;
    }
}