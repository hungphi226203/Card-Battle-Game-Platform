package com.web_game.Payment_Service.Repository;

import com.web_game.common.Entity.PaymentTransaction;
import com.web_game.common.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    PaymentTransaction findByOrderId(String id);
    List<PaymentTransaction> findAllByUserOrderByCreatedAtDesc(User user);
    List<PaymentTransaction> findAllByOrderByCreatedAtDesc();

    @Query(value = "SELECT DATE(created_at) as date, SUM(amountvnd) as total " +
            "FROM payment_transaction " +
            "WHERE status = 'SUCCESS' " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY DATE(created_at)", nativeQuery = true)
    List<Object[]> getRevenueByDateRaw();
}
