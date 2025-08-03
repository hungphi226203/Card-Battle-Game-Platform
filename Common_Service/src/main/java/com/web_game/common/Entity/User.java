package com.web_game.common.Entity;

import com.web_game.common.Enum.Gender;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    private String phone;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private LocalDate dob;

    @Column(name = "balance", nullable = false)
    private Float balance;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private String image;

    @Column(nullable = false)
    private boolean locked = false;

    @OneToMany(mappedBy = "user")
    private List<Inventory> inventory;

    @OneToMany(mappedBy = "seller")
    private List<Transaction> sellerTransactions;

    @OneToMany(mappedBy = "buyer")
    private List<Transaction> buyerTransactions;

    @OneToMany(mappedBy = "user")
    private List<Notification> notifications;
}