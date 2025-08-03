package com.web_game.Event_Service.Service;

import com.web_game.common.DTO.Respone.GachaResponse;

import java.util.List;

public interface EventService {
    GachaResponse performSingleGacha(Long userId);
    List<GachaResponse> performMultiGacha(Long userId);
}