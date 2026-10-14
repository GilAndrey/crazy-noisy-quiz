package com.crazynoisyquiz.backend.match.controller;

import com.crazynoisyquiz.backend.auth.service.JwtService;
import com.crazynoisyquiz.backend.match.dto.MatchRankingResponse;
import com.crazynoisyquiz.backend.match.dto.MatchResultResponse;
import com.crazynoisyquiz.backend.match.model.MatchStatus;
import com.crazynoisyquiz.backend.match.service.MatchService;
import com.crazynoisyquiz.backend.shared.exception.ForbiddenOperationException;
import com.crazynoisyquiz.backend.shared.exception.GlobalExceptionHandler;
import com.crazynoisyquiz.backend.shared.exception.ResourceConflictException;
import com.crazynoisyquiz.backend.user.model.AvatarKey;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MatchResultController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MatchResultControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private MatchService service;
    @MockitoBean private JwtService jwtService;

    @Test
    void shouldReturn200WithFinalRanking() throws Exception {
        UUID matchId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant finishedAt = Instant.parse("2026-10-14T15:12:26Z");
        when(service.findResult(matchId, "gil@email.com"))
                .thenReturn(new MatchResultResponse(matchId, "ABC123", MatchStatus.FINISHED, 5,
                        finishedAt, List.of(
                        new MatchRankingResponse(userId, "gil", AvatarKey.AVATAR_01, 1, 200, 3),
                        new MatchRankingResponse(UUID.randomUUID(), "labubu", null, 1, 200, 3))));

        // O GET devolve o ranking com empate e sem expor e-mail ou senha do jogador.
        mockMvc.perform(get("/api/matches/{matchId}/result", matchId)
                        .principal(new UsernamePasswordAuthenticationToken("gil@email.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(matchId.toString()))
                .andExpect(jsonPath("$.roomCode").value("ABC123"))
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.totalRounds").value(5))
                .andExpect(jsonPath("$.finishedAt").value(finishedAt.toString()))
                .andExpect(jsonPath("$.ranking.length()").value(2))
                .andExpect(jsonPath("$.ranking[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$.ranking[0].username").value("gil"))
                .andExpect(jsonPath("$.ranking[0].avatarKey").value("AVATAR_01"))
                .andExpect(jsonPath("$.ranking[0].position").value(1))
                .andExpect(jsonPath("$.ranking[1].position").value(1))
                .andExpect(jsonPath("$.ranking[0].totalPoints").value(200))
                .andExpect(jsonPath("$.ranking[0].correctAnswers").value(3))
                .andExpect(jsonPath("$.ranking[0].email").doesNotExist())
                .andExpect(jsonPath("$.ranking[0].passwordHash").doesNotExist());
        verify(service).findResult(matchId, "gil@email.com");
    }

    private void failure(RuntimeException error, int statusCode) throws Exception {
        UUID matchId = UUID.randomUUID();
        when(service.findResult(matchId, "player@email.com")).thenThrow(error);
        mockMvc.perform(get("/api/matches/{matchId}/result", matchId)
                        .principal(new UsernamePasswordAuthenticationToken("player@email.com", null)))
                .andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.status").value(statusCode))
                .andExpect(jsonPath("$.message").value(error.getMessage()));
    }

    @Test
    void shouldReturn403ForNonParticipant() throws Exception {
        failure(new ForbiddenOperationException("Apenas participantes podem consultar o resultado"), 403);
    }

    @Test
    void shouldReturn409BeforeMatchFinishes() throws Exception {
        failure(new ResourceConflictException("O resultado só está disponível após o término da partida"), 409);
    }

    @Test
    void shouldReturn404ForMissingMatch() throws Exception {
        failure(new EntityNotFoundException("Partida não encontrada"), 404);
    }
}
