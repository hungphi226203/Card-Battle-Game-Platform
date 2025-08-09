package com.web_game.Inventory_Service.Controller;

import com.web_game.Inventory_Service.Service.DeckService;
import com.web_game.common.DTO.Request.Deck.AddCardToDeckRequest;
import com.web_game.common.DTO.Request.Deck.RemoveCardFromDeckRequest;
import com.web_game.common.DTO.Request.Deck.UpdateDeckRequest;
import com.web_game.common.DTO.Respone.ApiResponse;
import com.web_game.common.DTO.Respone.CollectionResponse;
import com.web_game.common.DTO.Respone.DeckResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/deck")
public class DeckController {

    @Autowired
    private DeckService deckService;

    private boolean hasUserRole(HttpServletRequest request) {
        String roles = request.getHeader("X-Roles");
        return roles != null && roles.contains("USER");
    }

    private Long extractUserId(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-UserId");
        if (userIdHeader == null) {
            throw new IllegalArgumentException("Missing X-UserId header");
        }
        return Long.parseLong(userIdHeader);
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getUserDeck(HttpServletRequest request) {
        if (!hasUserRole(request)) {
            return ResponseEntity.status(403).body(
                    ApiResponse.builder().code(403).message("Forbidden: USER role required").build()
            );
        }

        Long userId = extractUserId(request);
        DeckResponse deck = deckService.getUserDeck(userId);
        return ResponseEntity.ok(
                ApiResponse.builder().code(200).message("Lấy deck thành công").data(deck).build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getOpponentDeck(@PathVariable("id") Long opponentId,
                                                       HttpServletRequest request) {
        if (!hasUserRole(request)) {
            return ResponseEntity.status(403).body(
                    ApiResponse.builder().code(403).message("Forbidden: USER role required").build()
            );
        }

        DeckResponse deck = deckService.getOpponentDeck(opponentId);
        return ResponseEntity.ok(
                ApiResponse.builder().code(200).message("Lấy deck của đối thủ thành công").data(deck).build()
        );
    }


    @PutMapping
    public ResponseEntity<ApiResponse> updateDeck(@Valid @RequestBody UpdateDeckRequest requestBody,
                                                  HttpServletRequest request) {
        if (!hasUserRole(request)) {
            return ResponseEntity.status(403).body(
                    ApiResponse.builder().code(403).message("Forbidden: USER role required").build()
            );
        }

        Long userId = extractUserId(request);
        DeckResponse deck = deckService.updateDeck(userId, requestBody);
        return ResponseEntity.ok(
                ApiResponse.builder().code(200).message("Cập nhật deck thành công").data(deck).build()
        );
    }

    @GetMapping("/collection")
    public ResponseEntity<ApiResponse> getUserCollection(HttpServletRequest request) {
        if (!hasUserRole(request)) {
            return ResponseEntity.status(403).body(
                    ApiResponse.builder().code(403).message("Forbidden: USER role required").build()
            );
        }

        Long userId = extractUserId(request);
        CollectionResponse collection = deckService.getUserCollection(userId);
        return ResponseEntity.ok(
                ApiResponse.builder().code(200).message("Lấy bộ sưu tập thành công").data(collection).build()
        );
    }
}