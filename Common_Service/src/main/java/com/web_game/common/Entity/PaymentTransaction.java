package com.web_game.common.Entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User user;

    @Column(name = "order_id")
    String orderId;

    @Column(name = "amountvnd")  // Explicit mapping
    Long amountVND;

    Integer diamonds;

    @Enumerated(EnumType.STRING)
    PaymentStatus status;

    @Column(name = "transaction_no")
    String transactionNo;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "confirmed_at")
    LocalDateTime confirmedAt;

    public enum PaymentStatus {
        PENDING, SUCCESS, FAILED
    }
}