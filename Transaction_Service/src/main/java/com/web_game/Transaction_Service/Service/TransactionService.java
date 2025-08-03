package com.web_game.Transaction_Service.Service;

import com.web_game.common.DTO.Request.Transaction.TransactionRequest;
import com.web_game.common.DTO.Respone.TransactionResponse;

import java.util.List;

public interface TransactionService {
    TransactionResponse createAndCompleteTransaction(TransactionRequest request, Long buyerUserId);

    List<TransactionResponse> getUserTransactions(Long userId, String role);

    TransactionResponse getTransaction(Long transactionId);

    TransactionResponse cancelTransaction(Long transactionId, Long userId);
}