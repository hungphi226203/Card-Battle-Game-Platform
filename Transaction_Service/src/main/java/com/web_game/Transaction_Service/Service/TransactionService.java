package com.web_game.Transaction_Service.Service;

import com.web_game.common.DTO.Request.Transaction.TransactionRequest;
import com.web_game.common.DTO.Respone.TransactionResponse;

public interface TransactionService {
    TransactionResponse createAndCompleteTransaction(TransactionRequest request, Long buyerUserId);

    TransactionResponse getTransaction(Long transactionId);

    TransactionResponse cancelTransaction(Long transactionId, Long userId);
}