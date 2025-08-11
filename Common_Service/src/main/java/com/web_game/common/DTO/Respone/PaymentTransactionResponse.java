package com.web_game.common.DTO.Respone;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentTransactionResponse {
    Long id;
    String orderId;
    Long amountVND;
    LocalDateTime createdAt;
    Integer diamonds;
    String status;
    String username;
}
