package com.crazynoisyquiz.backend.match.dto;

import com.crazynoisyquiz.backend.user.model.AvatarKey;

import java.util.UUID;

public record MatchRankingResponse(
        // Mostra a posição e o desempenho de um jogador na partida.
        UUID userId,
        String username,
        AvatarKey avatarKey,
        Integer position,
        Integer totalPoints,
        Integer correctAnswers
) {
}
