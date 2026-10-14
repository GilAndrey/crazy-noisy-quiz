package com.crazynoisyquiz.backend.match.dto;

import com.crazynoisyquiz.backend.match.model.MatchStatus;
import java.time.Instant;
import java.util.UUID;

public record MatchHistoryResponse(
        // Resume uma partida da sala sem carregar seu ranking inteiro.
        UUID matchId,
        MatchStatus status,
        Integer totalRounds,
        Integer currentRoundNumber,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt
) {
}
