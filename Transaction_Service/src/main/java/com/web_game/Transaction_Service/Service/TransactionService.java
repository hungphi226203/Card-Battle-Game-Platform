package com.web_game.Transaction_Service.Service;

import com.web_game.common.DTO.Request.Transaction.TransactionRequest;
import com.web_game.common.DTO.Respone.TransactionResponse;

public interface TransactionService {
    TransactionResponse createTransaction(TransactionRequest request, Long buyerUserId);

    TransactionResponse getTransaction(Long transactionId);

    TransactionResponse completeTransaction(Long transactionId);

    TransactionResponse cancelTransaction(Long transactionId, Long userId);
}