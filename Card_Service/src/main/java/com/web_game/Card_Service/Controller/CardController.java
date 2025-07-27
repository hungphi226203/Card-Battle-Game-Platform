package com.web_game.Card_Service.Controller;

import com.web_game.Card_Service.Service.CardService;
import com.web_game.common.DTO.Request.Card.CardCreateRequest;
import com.web_game.common.DTO.Request.Card.CardUpdateRequest;
import com.web_game.common.DTO.Request.Card.EffectCreateRequest;
import com.web_game.common.DTO.Request.Card.EffectUpdateRequest;
import com.web_game.common.DTO.Respone.ApiResponse;
import com.web_game.common.DTO.shared.CardDTO;
import com.web_game.common.DTO.shared.EffectDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    // ==================== CARD APIs ==================== //

    @GetMapping
    public ResponseEntity<ApiResponse> getAllCards() {
        List<CardDTO> cards = cardService.getAllCards();
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Lấy danh sách thẻ thành công")
                .data(cards)
                .build());
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<ApiResponse> getCard(@PathVariable Long cardId) {
        CardDTO card = cardService.getCard(cardId);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Lấy thông tin thẻ thành công")
                .data(card)
                .build());
    }

    // Lấy danh sách Effect để tạo thẻ
    @GetMapping("/create-info")
    public ResponseEntity<ApiResponse> getCreateCardInfo(HttpServletRequest request) {
        checkManagerRole(request);
        List<EffectDTO> effects = cardService.getAllEffects();
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Thông tin cần thiết để tạo thẻ")
                .data(Map.of("effects", effects))
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createCard(@Valid @RequestBody CardCreateRequest request,
                                                  HttpServletRequest httpRequest) {
        checkManagerRole(httpRequest);
        CardDTO card = cardService.createCard(request);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(201)
                .message("Tạo thẻ thành công")
                .data(card)
                .build());
    }

    @GetMapping("/{cardId}/update-info")
    public ResponseEntity<ApiResponse> getUpdateCardInfo(@PathVariable Long cardId,
                                                         HttpServletRequest request) {
        checkManagerRole(request);
        CardDTO card = cardService.getCard(cardId);
        List<EffectDTO> effects = cardService.getAllEffects();
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Thông tin cần thiết để cập nhật thẻ")
                .data(Map.of("card", card, "effects", effects))
                .build());
    }

    @PutMapping("/{cardId}")
    public ResponseEntity<ApiResponse> updateCard(@PathVariable Long cardId,
                                                  @Valid @RequestBody CardUpdateRequest request,
                                                  HttpServletRequest httpRequest) {
        checkManagerRole(httpRequest);
        CardDTO card = cardService.updateCard(cardId, request);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Cập nhật thẻ thành công")
                .data(card)
                .build());
    }

    @DeleteMapping("/{cardId}")
    public ResponseEntity<ApiResponse> deleteCard(@PathVariable Long cardId,
                                                  HttpServletRequest httpRequest) {
        checkManagerRole(httpRequest);
        cardService.deleteCard(cardId);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Xóa thẻ thành công")
                .data(null)
                .build());
    }

    // ==================== EFFECT APIs ==================== //

    @GetMapping("/effects")
    public ResponseEntity<ApiResponse> getAllEffects() {
        List<EffectDTO> effects = cardService.getAllEffects();
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Lấy danh sách hiệu ứng thành công")
                .data(effects)
                .build());
    }

    @GetMapping("/effects/{effectId}")
    public ResponseEntity<ApiResponse> getEffect(@PathVariable Long effectId) {
        EffectDTO effect = cardService.getEffect(effectId);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Lấy thông tin hiệu ứng thành công")
                .data(effect)
                .build());
    }

    @PostMapping("/effects")
    public ResponseEntity<ApiResponse> createEffect(@Valid @RequestBody EffectCreateRequest request,
                                                    HttpServletRequest httpRequest) {
        checkManagerRole(httpRequest);
        EffectDTO effect = cardService.createEffect(request);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(201)
                .message("Tạo hiệu ứng thành công")
                .data(effect)
                .build());
    }

    @PutMapping("/effects/{effectId}")
    public ResponseEntity<ApiResponse> updateEffect(@PathVariable Long effectId,
                                                    @Valid @RequestBody EffectUpdateRequest request,
                                                    HttpServletRequest httpRequest) {
        checkManagerRole(httpRequest);
        EffectDTO effect = cardService.updateEffect(effectId, request);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Cập nhật hiệu ứng thành công")
                .data(effect)
                .build());
    }

    @DeleteMapping("/effects/{effectId}")
    public ResponseEntity<ApiResponse> deleteEffect(@PathVariable Long effectId,
                                                    HttpServletRequest httpRequest) {
        checkManagerRole(httpRequest);
        cardService.deleteEffect(effectId);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Xóa hiệu ứng thành công")
                .data(null)
                .build());
    }

    // ==================== HELPER ==================== //
    private void checkManagerRole(HttpServletRequest request) {
        String rolesHeader = request.getHeader("X-Roles");
        if (rolesHeader == null || Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .noneMatch(role -> role.equalsIgnoreCase("MANAGER"))) {
            throw new AccessDeniedException("Bạn không có quyền MANAGER để thực hiện thao tác này.");
        }
    }
}