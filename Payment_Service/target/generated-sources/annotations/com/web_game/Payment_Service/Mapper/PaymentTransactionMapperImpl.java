package com.web_game.Payment_Service.Mapper;

import com.web_game.common.DTO.Respone.PaymentTransactionResponse;
import com.web_game.common.Entity.PaymentTransaction;
import com.web_game.common.Entity.User;
import javax.annotation.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-08-04T00:25:04+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.8 (Oracle Corporation)"
)
@Component
public class PaymentTransactionMapperImpl implements PaymentTransactionMapper {

    @Override
    public PaymentTransactionResponse toPaymentTransactionResponse(PaymentTransaction transaction) {
        if ( transaction == null ) {
            return null;
        }

        PaymentTransactionResponse.PaymentTransactionResponseBuilder paymentTransactionResponse = PaymentTransactionResponse.builder();

        paymentTransactionResponse.username( transactionUserUsername( transaction ) );
        if ( transaction.getStatus() != null ) {
            paymentTransactionResponse.status( transaction.getStatus().name() );
        }
        paymentTransactionResponse.id( transaction.getId() );
        paymentTransactionResponse.orderId( transaction.getOrderId() );
        paymentTransactionResponse.amountVND( transaction.getAmountVND() );
        paymentTransactionResponse.createdAt( transaction.getCreatedAt() );
        paymentTransactionResponse.diamonds( transaction.getDiamonds() );

        return paymentTransactionResponse.build();
    }

    private String transactionUserUsername(PaymentTransaction paymentTransaction) {
        if ( paymentTransaction == null ) {
            return null;
        }
        User user = paymentTransaction.getUser();
        if ( user == null ) {
            return null;
        }
        String username = user.getUsername();
        if ( username == null ) {
            return null;
        }
        return username;
    }
}
