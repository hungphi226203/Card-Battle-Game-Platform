package com.web_game.Inventory_Service.Controller;

import com.web_game.Inventory_Service.Service.InventoryService;
import com.web_game.common.DTO.Request.UserCard.SellCardRequest;
import com.web_game.common.DTO.Request.UserCard.UserCardCreateRequest;
import com.web_game.common.DTO.Respone.ApiResponse;
import com.web_game.common.DTO.Respone.InventoryResponse;
import com.web_game.common.DTO.shared.UserCardDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    private boolean hasRole(HttpServletRequest request, String requiredRole) {
        String roles = request.getHeader("X-Roles");
        return roles != null && roles.contains(requiredRole);
    }

    private Long extractUserId(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-UserId");
        if (userIdHeader == null) {
            throw new IllegalArgumentException("Missing X-UserId header");
        }
        return Long.parseLong(userIdHeader);
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getInventory(HttpServletRequest request) {
        if (!hasRole(request, "USER")) {
            return ResponseEntity.status(403).body(ApiResponse.builder()
                    .code(403)
                    .message("Forbidden: USER role required")
                    .build());
        }

        Long userId = extractUserId(request);
        List<UserCardDTO> cards = inventoryService.getInventory(userId);

        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Lấy danh sách thẻ trong kho thành công")
                .data(cards)
                .build());
    }

    @GetMapping("/card/{cardId}")
    public ResponseEntity<ApiResponse> getCardInInventory(@PathVariable Long cardId,
                                                          HttpServletRequest request) {
        if (!hasRole(request, "USER")) {
            return ResponseEntity.status(403).body(ApiResponse.builder()
                    .code(403)
                    .message("Forbidden: USER role required")
                    .build());
        }

        Long userId = extractUserId(request);
        UserCardDTO card = inventoryService.getCardInInventory(userId, cardId);

        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Lấy thông tin thẻ trong kho thành công")
                .data(card)
                .build());
    }

    @GetMapping("/item/{inventoryId}")
    public ResponseEntity<ApiResponse> getCardByInventoryId(@PathVariable Long inventoryId,
                                                            HttpServletRequest request) {
        // Không cần check role nữa vì bạn muốn user nào cũng truy cập được
        UserCardDTO card = inventoryService.getCardByInventoryId(inventoryId);

        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Lấy thông tin thẻ theo inventoryId thành công")
                .data(card)
                .build());
    }

    @PostMapping("/{userId}/cards")
    public ResponseEntity<ApiResponse> addCardToInventory(@PathVariable Long userId,
                                                          @Valid @RequestBody UserCardCreateRequest request,
                                                          HttpServletRequest httpRequest) {
        if (!hasRole(httpRequest, "MANAGER")) {
            return ResponseEntity.status(403).body(ApiResponse.builder()
                    .code(403)
                    .message("Forbidden: MANAGER role required")
                    .build());
        }

        Long callerId = extractUserId(httpRequest);
        if (callerId.equals(userId)) {
            return ResponseEntity.status(403).body(ApiResponse.builder()
                    .code(403)
                    .message("Forbidden: Cannot modify own inventory")
                    .build());
        }

        inventoryService.addCardToInventory(userId, request.getCardId(), "MANAGER:" + callerId);

        return ResponseEntity.ok(ApiResponse.builder()
                .code(201)
                .message("Thêm thẻ vào kho thành công")
                .build());
    }

    @PostMapping("/{userId}/cards/system")
    public ResponseEntity<ApiResponse> addCardToInventoryBySystem(@PathVariable Long userId,
                                                                  @Valid @RequestBody UserCardCreateRequest request,
                                                                  HttpServletRequest requestHeader) {
//        if (!hasRole(requestHeader, "SYSTEM")) {
//            return ResponseEntity.status(403).body(ApiResponse.builder()
//                    .code(403)
//                    .message("Forbidden: SYSTEM role required")
//                    .build());
//        }

        inventoryService.addCardToInventory(userId, request.getCardId(), "SYSTEM");

        return ResponseEntity.ok(ApiResponse.builder()
                .code(201)
                .message("Thêm thẻ vào kho thành công (hệ thống)")
                .build());
    }

    @DeleteMapping("/{userId}/cards/{cardId}")
    public ResponseEntity<ApiResponse> removeCardFromInventory(@PathVariable Long userId,
                                                               @PathVariable Long cardId,
                                                               HttpServletRequest httpRequest) {
        if (!hasRole(httpRequest, "MANAGER")) {
            return ResponseEntity.status(403).body(ApiResponse.builder()
                    .code(403)
                    .message("Forbidden: MANAGER role required")
                    .build());
        }

        Long callerId = extractUserId(httpRequest);
        if (callerId.equals(userId)) {
            return ResponseEntity.status(403).body(ApiResponse.builder()
                    .code(403)
                    .message("Forbidden: Cannot remove from own inventory")
                    .build());
        }

        inventoryService.removeCardFromInventory(userId, cardId, "MANAGER:" + callerId);

        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Xóa thẻ khỏi kho thành công")
                .build());
    }

    @PutMapping("/{inventoryId}/sell")
    public ResponseEntity<ApiResponse> listCardForSale(@PathVariable Long inventoryId,
                                                       @Valid @RequestBody SellCardRequest request,
                                                       HttpServletRequest httpRequest) {
        if (!hasRole(httpRequest, "USER")) {
            return ResponseEntity.status(403).body(ApiResponse.builder()
                    .code(403)
                    .message("Forbidden: USER role required")
                    .build());
        }

        Long userId = extractUserId(httpRequest);
        inventoryService.listCardForSale(inventoryId, request, userId);

        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Rao bán thẻ thành công")
                .build());
    }

    @PutMapping("/{inventoryId}/cancel-sell")
    public ResponseEntity<ApiResponse> cancelCardSale(@PathVariable Long inventoryId,
                                                      HttpServletRequest httpRequest) {
        if (!hasRole(httpRequest, "USER")) {
            return ResponseEntity.status(403).body(ApiResponse.builder()
                    .code(403)
                    .message("Forbidden: USER role required")
                    .build());
        }

        Long userId = extractUserId(httpRequest);
        inventoryService.cancelCardSale(inventoryId, userId);

        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Hủy rao bán thẻ thành công")
                .build());
    }

    @GetMapping("/for-sale")
    public ResponseEntity<ApiResponse> getCardsForSale(HttpServletRequest request) {

        List<InventoryResponse> cards = inventoryService.getCardsForSale();

        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Lấy danh sách thẻ đang rao bán thành công")
                .data(cards)
                .build());
    }

    @GetMapping("/my-for-sale")
    public ResponseEntity<ApiResponse> getMyCardsForSale(HttpServletRequest httpRequest) {
        if (!hasRole(httpRequest, "USER")) {
            return ResponseEntity.status(403).body(ApiResponse.builder()
                    .code(403)
                    .message("Forbidden: USER role required")
                    .build());
        }

        Long userId = extractUserId(httpRequest);
        List<InventoryResponse> myCards = inventoryService.getMyCardsForSale(userId);

        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Lấy danh sách thẻ đang rao bán của bạn thành công")
                .data(myCards)
                .build());
    }

}