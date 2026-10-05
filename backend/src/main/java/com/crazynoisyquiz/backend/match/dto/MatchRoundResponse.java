package com.crazynoisyquiz.backend.match.dto;

import com.crazynoisyquiz.backend.match.model.RoundStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MatchRoundResponse(
        UUID id,
        UUID matchId,
        Integer roundNumber,
        RoundStatus status,
        Instant startedAt,
        UUID questionId,
        String statement,
        Integer timeLimitSeconds,
        List<QuestionOptionResponse> options
) {
}
