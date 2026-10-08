package com.crazynoisyquiz.backend.match.controller;

import com.crazynoisyquiz.backend.match.dto.SubmitAnswerRequest;
import com.crazynoisyquiz.backend.match.dto.SubmitAnswerResponse;
import com.crazynoisyquiz.backend.match.service.MatchAnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/matches/{matchId}/rounds/{roundId}/answers")
@RequiredArgsConstructor
public class MatchAnswerController {
    // Recebe a alternativa escolhida pelo jogador autenticado.

    private final MatchAnswerService matchAnswerService;

    @PostMapping
    public ResponseEntity<SubmitAnswerResponse> submitAnswer(
            @PathVariable UUID matchId,
            @PathVariable UUID roundId,
            @Valid @RequestBody SubmitAnswerRequest request,
            Authentication authentication
    ) {
        SubmitAnswerResponse response = matchAnswerService.submitAnswer(
                matchId, roundId, authentication.getName(), request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
