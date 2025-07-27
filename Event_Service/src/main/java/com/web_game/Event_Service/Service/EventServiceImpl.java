package com.web_game.Event_Service.Service;

import com.web_game.Event_Service.Repository.CardRepository;
import com.web_game.Event_Service.Repository.GachaHistoryRepository;
import com.web_game.common.DTO.Respone.GachaResponse;
import com.web_game.common.Entity.Card;
import com.web_game.common.Entity.GachaHistory;
import com.web_game.common.Event.GachaEvent;
import com.web_game.common.Exception.AppException;
import com.web_game.common.Exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class EventServiceImpl implements EventService{
    @Autowired
    private GachaHistoryRepository gachaHistoryRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private KafkaTemplate<String, GachaEvent> kafkaTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String CARDS_CACHE_KEY = "cards_list";

    public GachaResponse performGacha(Long userId) {
        List<Card> cards = (List<Card>) redisTemplate.opsForValue().get(CARDS_CACHE_KEY);
        if (cards == null) {
            cards = cardRepository.findAll();
            if (cards.isEmpty()) {
                throw new AppException(ErrorCode.INVALID_CARD_VALUE);
            }
            redisTemplate.opsForValue().set(CARDS_CACHE_KEY, cards, 1, TimeUnit.HOURS);
        }

        Random random = new Random();
        Card selectedCard = cards.get(random.nextInt(cards.size()));
        Long cardId = selectedCard.getCardId();

        GachaHistory gachaHistory = new GachaHistory();
        gachaHistory.setUserId(userId);
        gachaHistory.setCardId(cardId);
        gachaHistory.setTimestamp(LocalDateTime.now());
        gachaHistoryRepository.save(gachaHistory);

        GachaEvent event = new GachaEvent();
        event.setUserId(userId);
        event.setCardId(cardId);
        event.setTimestamp(LocalDateTime.now());
        kafkaTemplate.send("gacha-events", event);

        GachaResponse response = new GachaResponse();
        response.setId(gachaHistory.getId());
        response.setUserId(userId);
        response.setCardId(cardId);
        response.setCardName(selectedCard.getName());
        return response;
    }
}