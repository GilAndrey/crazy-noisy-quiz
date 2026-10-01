package com.crazynoisyquiz.backend.match.model;

import com.crazynoisyquiz.backend.room.model.QuizRoom;
import com.crazynoisyquiz.backend.question.model.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.JoinTable;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;
import java.util.Set;

// Guarda uma partida realizada dentro de uma sala, incluindo seu estado e progresso.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private QuizRoom room;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "match_categories",
            joinColumns = @JoinColumn(name = "match_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchStatus status;

    @Column(name = "total_rounds", nullable = false)
    private Integer totalRounds;

    @Column(name = "current_round_number")
    private Integer currentRoundNumber;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = MatchStatus.IN_PROGRESS;
        }
        if (currentRoundNumber == null) {
            currentRoundNumber = 0;
        }
        if (startedAt == null) {
            startedAt = Instant.now();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
