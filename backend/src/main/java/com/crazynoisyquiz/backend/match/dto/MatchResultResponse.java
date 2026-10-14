package com.crazynoisyquiz.backend.match.dto;

import com.crazynoisyquiz.backend.match.model.MatchStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MatchResultResponse(
        // Reúne o resumo da partida encerrada e seu ranking final.
        UUID matchId,
        String roomCode,
        MatchStatus status,
        Integer totalRounds,
        Instant finishedAt,
        List<MatchRankingResponse> ranking
) {
}
