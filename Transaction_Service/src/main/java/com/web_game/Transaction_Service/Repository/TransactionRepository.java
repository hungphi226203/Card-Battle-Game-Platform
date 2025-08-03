package com.web_game.Transaction_Service.Repository;

import com.web_game.common.Entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByBuyerId(Long buyerId);

    List<Transaction> findBySellerId(Long sellerId);

    List<Transaction> findByBuyerIdOrSellerId(Long buyerId, Long sellerId);

}