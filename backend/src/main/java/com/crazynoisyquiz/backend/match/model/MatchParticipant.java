package com.crazynoisyquiz.backend.match.model;

import com.crazynoisyquiz.backend.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

// Registra quem participou da partida e guarda seu resultado individual.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "match_participants",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_match_participants_match_user",
                columnNames = {"match_id", "user_id"}
        )
)
public class MatchParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_points", nullable = false)
    private Integer totalPoints;

    @Column(name = "correct_answers", nullable = false)
    private Integer correctAnswers;

    private Integer position;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "disconnected_at")
    private Instant disconnectedAt;

    @PrePersist
    protected void onCreate() {
        if (totalPoints == null) {
            totalPoints = 0;
        }
        if (correctAnswers == null) {
            correctAnswers = 0;
        }
        if (joinedAt == null) {
            joinedAt = Instant.now();
        }
    }
}
