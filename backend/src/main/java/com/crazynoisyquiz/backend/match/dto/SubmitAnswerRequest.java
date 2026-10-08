package com.crazynoisyquiz.backend.match.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubmitAnswerRequest(
        // Recebe a alternativa escolhida pelo jogador.
        @NotNull(message = "Selecione uma alternativa")
        UUID optionId
) {
}
