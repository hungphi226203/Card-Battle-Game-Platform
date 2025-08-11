package com.web_game.Payment_Service.Controller;

import com.web_game.Payment_Service.Repository.PaymentTransactionRepository;
import com.web_game.Payment_Service.Service.PaymentService;
import com.web_game.common.DTO.Request.Payment.VnpayRequest;
import com.web_game.common.DTO.Respone.ApiResponse;
import com.web_game.common.DTO.Respone.PaymentTransactionResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/payments")
@AllArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentTransactionRepository paymentTransactionRepository;

    private Long getUserIdFromHeader(HttpServletRequest request) {
        String userIdStr = request.getHeader("X-UserId");
        return userIdStr != null ? Long.parseLong(userIdStr) : null;
    }

    private boolean hasRole(HttpServletRequest request, String role) {
        String rolesHeader = request.getHeader("X-Roles");
        if (rolesHeader == null) return false;
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .anyMatch(r -> r.equalsIgnoreCase(role));
    }

    private void checkRole(HttpServletRequest request, String... roles) {
        for (String role : roles) {
            if (hasRole(request, role)) return;
        }
        throw new AccessDeniedException("Bạn không có quyền truy cập");
    }

//    @PostMapping
//    public ResponseEntity<?> createPayment(@RequestBody VnpayRequest paymentRequest,
//                                           HttpServletRequest request) {
//        Long userId = getUserIdFromHeader(request);
//        if (userId == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(ApiResponse.builder()
//                            .code(401)
//                            .message("Thiếu X-UserId trong header")
//                            .build());
//        }
//
//        try {
//
//            String paymentUrl = paymentService.createPayment(paymentRequest);
//            return ResponseEntity.ok(
//                    ApiResponse.builder()
//                            .code(200)
//                            .message("Tạo liên kết thanh toán thành công")
//                            .data(paymentUrl)
//                            .build()
//            );
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body(ApiResponse.builder()
//                            .code(400)
//                            .message(e.getMessage())
//                            .build());
//        } catch (Exception e) {
//            log.error("Lỗi khi tạo thanh toán", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(ApiResponse.builder()
//                            .code(500)
//                            .message("Đã xảy ra lỗi khi tạo thanh toán!")
//                            .build());
//        }
//    }

//    @GetMapping("/return")
//    public ResponseEntity<?> returnPayment(HttpServletRequest request) throws UnsupportedEncodingException {
//        Map<String, String> params = request.getParameterMap().entrySet().stream()
//                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0]));
//
//        log.info("VNPay return raw params: {}", params);
//        ResponseEntity<String> response = paymentService.handlePaymentReturn(params);
//
//        String redirectUrl = "http://localhost:3000/payment"; // Thay bằng URL frontend của bạn
//        if (response.getStatusCode() == HttpStatus.OK) {
//            redirectUrl += "?status=success&message=" + URLEncoder.encode("Thanh toán thành công", StandardCharsets.UTF_8.toString());
//        } else {
//            redirectUrl += "?status=error&message=" + URLEncoder.encode(response.getBody(), StandardCharsets.UTF_8.toString());
//        }
//
//        return ResponseEntity.status(HttpStatus.FOUND)
//                .header(HttpHeaders.LOCATION, redirectUrl)
//                .build();
//    }
//
//    @GetMapping("/myPaymentTransactions")
//    public ResponseEntity<ApiResponse> getMyTransactions(HttpServletRequest request) {
//        Long userId = getUserIdFromHeader(request);
//        if (userId == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(ApiResponse.builder()
//                            .code(401)
//                            .message("Thiếu X-UserId trong header")
//                            .build());
//        }
//
//        // Nếu cần, checkRole(request, "USER", "ADMIN");
//
//        List<PaymentTransactionResponse> transactions = paymentService.getMyTransactions();
//        return ResponseEntity.ok(
//                ApiResponse.builder()
//                        .code(200)
//                        .message("Lấy lịch sử giao dịch thành công")
//                        .data(transactions)
//                        .build()
//        );
//    }
//
//    @GetMapping("/allPaymentTransactions")
//    public ResponseEntity<ApiResponse> getAllTransactions(HttpServletRequest request) {
//        // Chỉ admin hoặc manager được xem tất cả giao dịch
//        try {
//            checkRole(request, "ADMIN", "MANAGER");
//        } catch (AccessDeniedException ex) {
//            return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                    .body(ApiResponse.builder()
//                            .code(403)
//                            .message("Bạn không có quyền truy cập danh sách giao dịch")
//                            .build());
//        }
//
//        List<PaymentTransactionResponse> transactions = paymentService.getAllTransactions();
//        return ResponseEntity.ok(
//                ApiResponse.builder()
//                        .code(200)
//                        .message("Lấy tất cả giao dịch thành công")
//                        .data(transactions)
//                        .build()
//        );
//    }
//
//    @GetMapping("/revenue-by-date")
//    public ResponseEntity<ApiResponse> getRevenueByDate(HttpServletRequest request) {
//        try {
//            checkRole(request, "ADMIN", "MANAGER");
//        } catch (AccessDeniedException ex) {
//            return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                    .body(ApiResponse.builder()
//                            .code(403)
//                            .message("Bạn không có quyền xem doanh thu")
//                            .build());
//        }
//
//        try {
//            List<Map<String, Object>> data = paymentService.getRevenueByDate();
//            return ResponseEntity.ok(
//                    ApiResponse.builder()
//                            .code(200)
//                            .message("Lấy doanh thu thành công")
//                            .data(data)
//                            .build()
//            );
//        } catch (Exception e) {
//            log.error("Lỗi khi lấy doanh thu", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(ApiResponse.builder()
//                            .code(500)
//                            .message("Lỗi khi lấy doanh thu: " + e.getMessage())
//                            .build());
//        }
//    }
@PostMapping
public ResponseEntity<ApiResponse<String>> createPayment(@RequestBody VnpayRequest paymentRequest) {
    try {
        String paymentUrl = paymentService.createPayment(paymentRequest);
        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .code(200)
                        .message("Tạo thanh toán thành công")
                        .data(paymentUrl)
                        .build()
        );
    } catch (IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse.<String>builder()
                        .code(400)
                        .message(e.getMessage())
                        .build()
        );
    } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
    }
}
    @GetMapping("/return")
    public ResponseEntity<?> returnPayment(HttpServletRequest request) throws UnsupportedEncodingException {
        System.out.println("In returnPayment method");
        Map<String, String> params = request.getParameterMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0]));

        log.info("VNPay return raw params: {}", params);
        ResponseEntity<String> response = paymentService.handlePaymentReturn(params);

        // Tạo redirect URL với thông báo
        String redirectUrl = "http://192.168.1.41:3000/payment"; // Thay bằng URL frontend của bạn
        if (response.getStatusCode() == HttpStatus.OK) {
            redirectUrl += "?status=success&message=" + URLEncoder.encode("Thanh toán thành công", StandardCharsets.UTF_8.toString());
        } else {
            redirectUrl += "?status=error&message=" + URLEncoder.encode(response.getBody(), StandardCharsets.UTF_8.toString());
        }
        System.out.println("Redirect URL: " + redirectUrl);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirectUrl)
                .build();
    }
    @GetMapping("/myPaymentTransactions")
    public ResponseEntity<ApiResponse<List<PaymentTransactionResponse>>> getMyTransactions() {
        try {
            List<PaymentTransactionResponse> transactions = paymentService.getMyTransactions();
            return ResponseEntity.ok(
                    ApiResponse.<List<PaymentTransactionResponse>>builder()
                            .code(HttpStatus.OK.value())
                            .message("Lấy lịch sử giao dịch thành công")
                            .data(transactions)
                            .build()
            );
        } catch (Exception e) {
            log.error("Lỗi khi lấy lịch sử giao dịch: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<List<PaymentTransactionResponse>>builder()
                            .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("Lỗi khi lấy lịch sử giao dịch")
                            .data(null)
                            .build()
            );
        }
    }

    @GetMapping("/allPaymentTransactions")
    public ResponseEntity<ApiResponse<List<PaymentTransactionResponse>>> getAllTransactions() {
        try {
            List<PaymentTransactionResponse> transactions = paymentService.getAllTransactions();
            return ResponseEntity.ok(
                    ApiResponse.<List<PaymentTransactionResponse>>builder()
                            .code(HttpStatus.OK.value())
                            .message("Lấy tất cả giao dịch thành công")
                            .data(transactions)
                            .build()
            );
        } catch (Exception e) {
            log.error("Lỗi khi lấy tất cả giao dịch: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<List<PaymentTransactionResponse>>builder()
                            .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("Lỗi khi lấy tất cả giao dịch")
                            .data(null)
                            .build()
            );
        }
    }

    @GetMapping("/revenue-by-date")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRevenueByDate() {
        try {
            List<Map<String, Object>> revenueData = paymentService.getRevenueByDate();
            return ResponseEntity.ok(
                    ApiResponse.<List<Map<String, Object>>>builder()
                            .code(HttpStatus.OK.value())
                            .message("Lấy doanh thu theo ngày thành công")
                            .data(revenueData)
                            .build()
            );
        } catch (Exception e) {
            log.error("Lỗi khi lấy doanh thu: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<List<Map<String, Object>>>builder()
                            .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("Lỗi khi lấy doanh thu theo ngày")
                            .data(null)
                            .build()
            );
        }
    }
}
