package com.web_game.common.DTO.Request.Payment;

import lombok.Data;

@Data
public class VnpayRequest {
    private String amount;
    private int diamonds;
}
