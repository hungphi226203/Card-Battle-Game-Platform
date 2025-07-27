package com.web_game.Transaction_Service.Repository;

import com.web_game.common.Entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}