package com.crazynoisyquiz.backend.match.dto;

import com.crazynoisyquiz.backend.match.model.MatchStatus;

import java.util.List;
import java.util.UUID;

public record MatchResponse(
        UUID id,
        String roomCode,
        MatchStatus status,
        Integer totalRounds,
        Integer currentRoundNumber,
        List<UUID> categoryIds
) {
}
