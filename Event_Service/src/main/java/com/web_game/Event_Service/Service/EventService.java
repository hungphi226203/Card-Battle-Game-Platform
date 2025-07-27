package com.web_game.Event_Service.Service;

import com.web_game.common.DTO.Respone.GachaResponse;

public interface EventService {
    public GachaResponse performGacha(Long userId);
}