package com.web_game.Payment_Service.Service;

import com.web_game.Payment_Service.Config.VNPayConfig;
import com.web_game.Payment_Service.Mapper.PaymentTransactionMapper;
import com.web_game.Payment_Service.Repository.PaymentTransactionRepository;
import com.web_game.Payment_Service.Repository.UserRepository;
import com.web_game.common.DTO.Request.Payment.VnpayRequest;
import com.web_game.common.DTO.Respone.PaymentTransactionResponse;
import com.web_game.common.Entity.PaymentTransaction;
import com.web_game.common.Entity.User;
import com.web_game.common.Exception.AppException;
import com.web_game.common.Exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final UserRepository userRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentTransactionMapper paymentTransactionMapper;

    public String createPayment(VnpayRequest paymentRequest) throws UnsupportedEncodingException {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String orderType = "other";

        long amount = 0;
        try {
            amount = Long.parseLong(paymentRequest.getAmount()) * 100;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Số tiền không hợp lệ");
        }

        System.out.println("diamonds: " + paymentRequest.getDiamonds());

        String bankCode = "NCB";
        String vnp_TxnRef = VNPayConfig.getRandomNumber(8);
        String vnp_IpAddr = "127.0.0.1";
        String vnp_TmnCode = VNPayConfig.vnp_TmnCode;

        // Lấy user hiện tại từ context
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Tạo bản ghi giao dịch trước
        PaymentTransaction transaction = PaymentTransaction.builder()
                .user(user)
                .orderId(vnp_TxnRef)
                .amountVND(amount / 100)
                .diamonds(paymentRequest.getDiamonds())
                .status(PaymentTransaction.PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        paymentTransactionRepository.save(transaction);

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");

        vnp_Params.put("vnp_BankCode", bankCode);
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang:" + vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VNPayConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName).append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()))
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                query.append('&');
                hashData.append('&');
            }
        }

        if (query.length() > 0)
            query.setLength(query.length() - 1);
        if (hashData.length() > 0)
            hashData.setLength(hashData.length() - 1);

        String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.secretKey, hashData.toString());
        query.append("&vnp_SecureHash=").append(vnp_SecureHash);
        return VNPayConfig.vnp_PayUrl + "?" + query;
    }

    public ResponseEntity<String> handlePaymentReturn(Map<String, String> params) {
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        String txnRef = params.get("vnp_TxnRef"); // Mã đơn hàng của bạn
        String amountStr = params.get("vnp_Amount");
        String transactionNo = params.get("vnp_TransactionNo"); // Mã giao dịch từ VNPay

        // 1. Xác minh chữ ký (bắt buộc)
        boolean isValid = verifySignature(params); // bạn cần viết verifySignature

        if (!isValid) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Sai chữ ký xác minh!");
        }
        // Tìm transaction theo mã đơn
        PaymentTransaction transaction = paymentTransactionRepository.findByOrderId(txnRef);

        if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
            Long amountVND = Long.parseLong(amountStr) / 100;
            int diamonds = transaction.getDiamonds();
            System.out.println("amountVND: " + amountVND + ", diamonds: " + diamonds + ", transactionNo: " + transactionNo);
            // Cập nhật transaction
            transaction.setAmountVND(amountVND);
            transaction.setDiamonds(diamonds);
            transaction.setTransactionNo(transactionNo);
            transaction.setConfirmedAt(LocalDateTime.now());
            transaction.setStatus(PaymentTransaction.PaymentStatus.SUCCESS);
            paymentTransactionRepository.save(transaction);

            // Cập nhật số dư user
            User user = transaction.getUser();
            user.setBalance(user.getBalance() + diamonds);
            log.info("Thông tin user: " + user.toString());
            userRepository.save(user);

            return ResponseEntity.ok("Thanh toán thành công!");
        } else {
            transaction.setStatus(PaymentTransaction.PaymentStatus.FAILED);
            paymentTransactionRepository.save(transaction);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Thanh toán thất bại! Mã lỗi: " + responseCode);
        }
    }

    public boolean verifySignature(Map<String, String> params) {
        Map<String, String> fields = new HashMap<>(params);
        String receivedHash = fields.remove("vnp_SecureHash");

        String signData = fields.entrySet().stream()
                .filter(e -> e.getKey().startsWith("vnp_"))
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    try {
                        return e.getKey() + "=" + URLEncoder.encode(e.getValue(), "US-ASCII");
                    } catch (UnsupportedEncodingException ex) {
                        throw new RuntimeException(ex);
                    }
                })
                .collect(Collectors.joining("&"));

        String myHash = VNPayConfig.hmacSHA512(VNPayConfig.secretKey, signData);

        log.info("🔐 signData dùng để ký: {}", signData);
        log.info("🔐 Chữ ký tính từ server (myHash): {}", myHash);
        log.info("🔐 Chữ ký VNPay gửi về (receivedHash): {}", receivedHash);

        return myHash.equalsIgnoreCase(receivedHash);
    }

    public List<PaymentTransactionResponse> getMyTransactions() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return paymentTransactionRepository.findAllByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(paymentTransactionMapper::toPaymentTransactionResponse)
                .collect(Collectors.toList());
    }

    public List<PaymentTransactionResponse> getAllTransactions() {
        return paymentTransactionRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(paymentTransactionMapper::toPaymentTransactionResponse)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getRevenueByDate() {
        List<Object[]> rawData = paymentTransactionRepository.getRevenueByDateRaw();

        return rawData.stream().map(row -> {
            Map<String, Object> map = new HashMap<>();
            map.put("date", row[0]);  // LocalDate
            map.put("totalRevenue", row[1]); // BigDecimal or Long (tùy DB)
            return map;
        }).collect(Collectors.toList());
    }
}
