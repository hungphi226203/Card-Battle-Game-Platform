package com.web_game.Event_Service.Controller;

import com.web_game.Event_Service.Service.EventService;
import com.web_game.common.DTO.Request.Event.GachaRequest;
import com.web_game.common.DTO.Respone.ApiResponse;
import com.web_game.common.DTO.Respone.GachaResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/event")
public class EventController {

    @Autowired
    private EventService eventService;

//    @PostMapping("/gacha/1")
//
//    @PostMapping("/gacha/10")

    @PostMapping("/gacha")
    public ResponseEntity<ApiResponse> performGacha(@Valid @RequestBody GachaRequest request,
                                                    Authentication authentication) {
        GachaResponse response = eventService.performGacha(Long.valueOf(authentication.getName()));
        return ResponseEntity.ok(ApiResponse.builder()
                .code(201)
                .message("Quay thẻ thành công")
                .data(response)
                .build());
    }
}