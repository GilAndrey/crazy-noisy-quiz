package com.crazynoisyquiz.backend.room.model;

import com.crazynoisyquiz.backend.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "quiz_rooms")
public class QuizRoom {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 6)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status;

    @Column(name = "max_players", nullable = false)
    private Integer maxPlayers;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = RoomStatus.WAITING;
        }
        if (maxPlayers == null) {
            maxPlayers = 8;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
