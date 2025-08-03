package com.web_game.Event_Service.Service;

import com.web_game.Event_Service.Repository.CardRepository;
import com.web_game.Event_Service.Repository.GachaHistoryRepository;
import com.web_game.common.DTO.Respone.GachaResponse;
import com.web_game.common.Entity.Card;
import com.web_game.common.Entity.GachaHistory;
import com.web_game.common.Enum.Rarity;
import com.web_game.common.Event.GachaEvent;
import com.web_game.common.Exception.AppException;
import com.web_game.common.Exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class EventServiceImpl implements EventService {

    @Autowired
    private GachaHistoryRepository gachaHistoryRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private KafkaTemplate<String, GachaEvent> kafkaTemplate;

    private static final double COMMON_RATE = 0.7;
    private static final double RARE_RATE = 0.2;
    private static final double EPIC_RATE = 0.09;
    private static final double LEGENDARY_RATE = 0.01;

    private List<Card> getCardsByRarity(Rarity rarity) {
        return cardRepository.findByRarity(rarity);
    }

    // Random card theo tỷ lệ
    private Card getRandomCard() {
        List<Card> common = getCardsByRarity(Rarity.COMMON);
        List<Card> rare = getCardsByRarity(Rarity.RARE);
        List<Card> epic = getCardsByRarity(Rarity.EPIC);
        List<Card> legendary = getCardsByRarity(Rarity.LEGENDARY);

        if (common.isEmpty() || rare.isEmpty() || epic.isEmpty() || legendary.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_CARD_VALUE);
        }

        double r = Math.random();
        Random rand = new Random();
        if (r < LEGENDARY_RATE) return legendary.get(rand.nextInt(legendary.size()));
        if (r < LEGENDARY_RATE + EPIC_RATE) return epic.get(rand.nextInt(epic.size()));
        if (r < LEGENDARY_RATE + EPIC_RATE + RARE_RATE) return rare.get(rand.nextInt(rare.size()));
        return common.get(rand.nextInt(common.size()));
    }

    // Lưu lịch sử + gửi Kafka + trả response
    private GachaResponse saveGacha(Long userId, Card card) {
        GachaHistory history = new GachaHistory();
        history.setUserId(userId);
        history.setCardId(card.getCardId());
        history.setTimestamp(LocalDateTime.now());
        gachaHistoryRepository.save(history);

        GachaEvent event = new GachaEvent();
        event.setUserId(userId);
        event.setCardId(card.getCardId());
        event.setTimestamp(LocalDateTime.now());
        kafkaTemplate.send("gacha-events", event);

        GachaResponse response = new GachaResponse();
        response.setId(history.getId());
        response.setUserId(userId);
        response.setCardId(card.getCardId());
        response.setCardName(card.getName());
        return response;
    }

    @Override
    public GachaResponse performSingleGacha(Long userId) {
        Card card = getRandomCard();
        return saveGacha(userId, card);
    }

    @Override
    public List<GachaResponse> performMultiGacha(Long userId) {
        List<Card> legendary = getCardsByRarity(Rarity.LEGENDARY);
        if (legendary.isEmpty()) throw new AppException(ErrorCode.INVALID_CARD_VALUE);

        List<GachaResponse> results = new ArrayList<>();
        boolean hasLegendary = false;

        for (int i = 0; i < 10; i++) {
            Card card = getRandomCard();
            if (card.getRarity() == Rarity.LEGENDARY) hasLegendary = true;
            results.add(saveGacha(userId, card));
        }

        if (!hasLegendary) {
            Card guaranteedLegendary = legendary.get(new Random().nextInt(legendary.size()));
            results.set(10 - 1, saveGacha(userId, guaranteedLegendary));
        }

        return results;
    }
}