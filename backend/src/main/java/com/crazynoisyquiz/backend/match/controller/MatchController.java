package com.crazynoisyquiz.backend.match.controller;

import com.crazynoisyquiz.backend.match.dto.MatchResponse;
import com.crazynoisyquiz.backend.match.dto.StartMatchRequest;
import com.crazynoisyquiz.backend.match.service.MatchService;
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

@RestController
@RequestMapping("/api/rooms/{roomCode}/matches")
@RequiredArgsConstructor
// Expõe o endpoint para o dono iniciar uma partida na sala.
public class MatchController {

    private final MatchService matchService;

    @PostMapping
    public ResponseEntity<MatchResponse> startMatch(
            @PathVariable String roomCode,
            @Valid @RequestBody StartMatchRequest request,
            Authentication authentication
    ) {
        MatchResponse response = matchService.startMatch(
                roomCode,
                authentication.getName(),
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


}
