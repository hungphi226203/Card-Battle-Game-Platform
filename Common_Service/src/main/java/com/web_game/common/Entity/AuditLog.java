package com.web_game.common.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action")
    private String action;

    @Column(name = "actor_username")
    private String actorUsername;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "card_id")
    private Long cardId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;
}