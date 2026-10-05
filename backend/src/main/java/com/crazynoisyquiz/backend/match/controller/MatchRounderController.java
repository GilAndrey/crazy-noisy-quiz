package com.crazynoisyquiz.backend.match.controller;

import com.crazynoisyquiz.backend.match.dto.MatchRoundResponse;
import com.crazynoisyquiz.backend.match.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/matches/{matchId}/rounds")
@RequiredArgsConstructor
public class MatchRounderController {

    private final MatchService matchService;

    @PostMapping("/next")
    public ResponseEntity<MatchRoundResponse> startNextRound(
            @PathVariable UUID matchId,
            Authentication authentication
    ) {
        MatchRoundResponse response = matchService.startNextRound(matchId, authentication.getName());

        return ResponseEntity.ok(response);
    }

    // Mostra a rodada atual para um participante da partida.
    @GetMapping("/current")
    public ResponseEntity<MatchRoundResponse> findCurrentRound(
            @PathVariable UUID matchId,
            Authentication authentication
    ) {
        MatchRoundResponse response = matchService.findCurrentRound(
                matchId,
                authentication.getName()
        );

        return ResponseEntity.ok(response);
    }
}
