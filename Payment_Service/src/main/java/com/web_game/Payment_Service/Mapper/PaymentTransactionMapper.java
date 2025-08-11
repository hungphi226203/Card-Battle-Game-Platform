package com.web_game.Payment_Service.Mapper;

import com.web_game.common.DTO.Respone.PaymentTransactionResponse;
import com.web_game.common.Entity.PaymentTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentTransactionMapper {
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "status", target = "status")
    PaymentTransactionResponse toPaymentTransactionResponse(PaymentTransaction transaction);
}
