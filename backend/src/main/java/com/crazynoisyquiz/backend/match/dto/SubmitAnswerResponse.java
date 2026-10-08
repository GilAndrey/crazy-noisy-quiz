package com.crazynoisyquiz.backend.match.dto;

import java.time.Instant;
import java.util.UUID;

public record SubmitAnswerResponse(   // Confirma a resposta registrada sem revelar se o jogador acertou.
        UUID id,
        UUID roundId,
        UUID optionId,
        Instant answeredAt
) {
}
