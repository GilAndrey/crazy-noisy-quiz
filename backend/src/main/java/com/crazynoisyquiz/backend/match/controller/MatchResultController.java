package com.crazynoisyquiz.backend.match.controller;

import com.crazynoisyquiz.backend.match.dto.MatchResultResponse;
import com.crazynoisyquiz.backend.match.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchResultController {
    // Entrega o resultado final para um participante autenticado.

    private final MatchService matchService;

    @GetMapping("/{matchId}/result")
    public ResponseEntity<MatchResultResponse> findResult(
            @PathVariable UUID matchId,
            Authentication authentication
    ) {
        MatchResultResponse response = matchService.findResult(
                matchId,
                authentication.getName()
        );

        return ResponseEntity.ok(response);
    }
}
