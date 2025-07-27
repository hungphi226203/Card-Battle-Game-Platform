package com.web_game.Transaction_Service.Controller;

import com.web_game.Transaction_Service.Service.TransactionService;
import com.web_game.common.DTO.Request.Transaction.TransactionRequest;
import com.web_game.common.DTO.Respone.ApiResponse;
import com.web_game.common.DTO.Respone.TransactionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    private boolean hasRole(HttpServletRequest request, String role) {
        String roles = request.getHeader("X-Roles");
        return roles != null && roles.contains(role);
    }

    private Long extractUserId(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-UserId");
        if (userIdHeader == null) return null;
        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createTransaction(@Valid @RequestBody TransactionRequest request,
                                                         HttpServletRequest httpRequest) {
        if (!hasRole(httpRequest, "USER")) {
            return ResponseEntity.status(403).body(ApiResponse.builder()
                    .code(403)
                    .message("Forbidden: USER role required")
                    .build());
        }

        Long userId = extractUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .code(400)
                    .message("Missing or invalid X-UserId header")
                    .build());
        }

        TransactionResponse response = transactionService.createTransaction(request, userId);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(201)
                .message("Tạo giao dịch thành công")
                .data(response)
                .build());
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse> getTransaction(@PathVariable Long transactionId,
                                                      HttpServletRequest httpRequest) {
        if (!hasRole(httpRequest, "USER")) {
            return ResponseEntity.status(403).body(ApiResponse.builder()
                    .code(403)
                    .message("Forbidden: USER role required")
                    .build());
        }

        TransactionResponse response = transactionService.getTransaction(transactionId);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Lấy thông tin giao dịch thành công")
                .data(response)
                .build());
    }

    @PutMapping("/{transactionId}/complete")
    public ResponseEntity<ApiResponse> completeTransaction(@PathVariable Long transactionId,
                                                           HttpServletRequest request) {
        if (!hasRole(request, "SYSTEM")) {
            return ResponseEntity.status(403).body(ApiResponse.builder()
                    .code(403)
                    .message("Forbidden: SYSTEM role required")
                    .build());
        }

        TransactionResponse response = transactionService.completeTransaction(transactionId);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Hoàn tất giao dịch thành công")
                .data(response)
                .build());
    }

    @PutMapping("/{transactionId}/cancel")
    public ResponseEntity<ApiResponse> cancelTransaction(@PathVariable Long transactionId,
                                                         HttpServletRequest httpRequest) {
        if (!hasRole(httpRequest, "USER")) {
            return ResponseEntity.status(403).body(ApiResponse.builder()
                    .code(403)
                    .message("Forbidden: USER role required")
                    .build());
        }

        Long userId = extractUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .code(400)
                    .message("Missing or invalid X-UserId header")
                    .build());
        }

        TransactionResponse response = transactionService.cancelTransaction(transactionId, userId);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(200)
                .message("Hủy giao dịch thành công")
                .data(response)
                .build());
    }
}