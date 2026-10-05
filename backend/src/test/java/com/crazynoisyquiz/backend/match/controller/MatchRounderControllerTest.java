package com.crazynoisyquiz.backend.match.controller;

import com.crazynoisyquiz.backend.auth.service.JwtService;
import com.crazynoisyquiz.backend.match.dto.MatchRoundResponse;
import com.crazynoisyquiz.backend.match.dto.QuestionOptionResponse;
import com.crazynoisyquiz.backend.match.model.RoundStatus;
import com.crazynoisyquiz.backend.match.service.MatchService;
import com.crazynoisyquiz.backend.shared.exception.ForbiddenOperationException;
import com.crazynoisyquiz.backend.shared.exception.GlobalExceptionHandler;
import com.crazynoisyquiz.backend.shared.exception.ResourceConflictException;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MatchRounderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MatchRounderControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private MatchService matchService;
    @MockitoBean private JwtService jwtService;

    @Test
    void shouldGetCurrentRoundWithoutRevealingAnswer() throws Exception {
        UUID matchId = UUID.randomUUID();
        UUID roundId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        Instant startedAt = Instant.parse("2026-10-05T17:54:57Z");
        when(matchService.findCurrentRound(matchId, "player@email.com"))
                .thenReturn(new MatchRoundResponse(roundId, matchId, 1,
                        RoundStatus.IN_PROGRESS, startedAt, questionId,
                        "Qual é o maior oceano da Terra?", 10,
                        List.of(new QuestionOptionResponse(optionId, "Oceano Pacífico", 3))));

        // O GET usa o participante autenticado e entrega a pergunta sem o gabarito.
        mockMvc.perform(get("/api/matches/{matchId}/rounds/current", matchId)
                        .principal(new UsernamePasswordAuthenticationToken("player@email.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(roundId.toString()))
                .andExpect(jsonPath("$.matchId").value(matchId.toString()))
                .andExpect(jsonPath("$.roundNumber").value(1))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.startedAt").value(startedAt.toString()))
                .andExpect(jsonPath("$.questionId").value(questionId.toString()))
                .andExpect(jsonPath("$.statement").value("Qual é o maior oceano da Terra?"))
                .andExpect(jsonPath("$.timeLimitSeconds").value(10))
                .andExpect(jsonPath("$.options[0].id").value(optionId.toString()))
                .andExpect(jsonPath("$.options[0].optionText").value("Oceano Pacífico"))
                .andExpect(jsonPath("$.options[0].optionOrder").value(3))
                .andExpect(jsonPath("$.options[0].isCorrect").doesNotExist())
                .andExpect(jsonPath("$.options[0].correct").doesNotExist());
        verify(matchService).findCurrentRound(matchId, "player@email.com");
    }

    @Test
    void shouldReturn403WhenCurrentRoundCallerIsNotParticipant() throws Exception {
        UUID matchId = UUID.randomUUID();
        when(matchService.findCurrentRound(matchId, "outsider@email.com"))
                .thenThrow(new ForbiddenOperationException("Apenas participantes podem consultar a rodada"));
        mockMvc.perform(get("/api/matches/{matchId}/rounds/current", matchId)
                        .principal(new UsernamePasswordAuthenticationToken("outsider@email.com", null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Apenas participantes podem consultar a rodada"));
    }

    @Test
    void shouldReturn409WhenNoRoundWasOpened() throws Exception {
        UUID matchId = UUID.randomUUID();
        when(matchService.findCurrentRound(matchId, "player@email.com"))
                .thenThrow(new ResourceConflictException("Nenhuma rodada foi aberta ainda"));
        // Antes de abrir a primeira rodada, o GET explica por que ainda não há pergunta.
        mockMvc.perform(get("/api/matches/{matchId}/rounds/current", matchId)
                        .principal(new UsernamePasswordAuthenticationToken("player@email.com", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Nenhuma rodada foi aberta ainda"));
    }

    @Test
    void shouldReturn404WhenCurrentRoundMatchDoesNotExist() throws Exception {
        UUID matchId = UUID.randomUUID();
        when(matchService.findCurrentRound(matchId, "player@email.com"))
                .thenThrow(new EntityNotFoundException("Partida não encontrada"));
        mockMvc.perform(get("/api/matches/{matchId}/rounds/current", matchId)
                        .principal(new UsernamePasswordAuthenticationToken("player@email.com", null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Partida não encontrada"));
    }

    @Test
    void shouldOpenRoundWithoutRevealingCorrectAnswer() throws Exception {
        UUID matchId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        when(matchService.startNextRound(matchId, "owner@email.com"))
                .thenReturn(new MatchRoundResponse(UUID.randomUUID(), matchId, 1,
                        RoundStatus.IN_PROGRESS, Instant.now(), UUID.randomUUID(),
                        "Quanto é 2 + 2?", 10,
                        List.of(new QuestionOptionResponse(optionId, "4", 1))));

        // O usuário vem da requisição; o JSON mostra a alternativa, mas nunca o gabarito.
        mockMvc.perform(post("/api/matches/{matchId}/rounds/next", matchId)
                        .principal(new UsernamePasswordAuthenticationToken("owner@email.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(matchId.toString()))
                .andExpect(jsonPath("$.roundNumber").value(1))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.statement").value("Quanto é 2 + 2?"))
                .andExpect(jsonPath("$.options[0].id").value(optionId.toString()))
                .andExpect(jsonPath("$.options[0].optionText").value("4"))
                .andExpect(jsonPath("$.options[0].isCorrect").doesNotExist())
                .andExpect(jsonPath("$.options[0].correct").doesNotExist());
        verify(matchService).startNextRound(matchId, "owner@email.com");
    }

    @Test
    void shouldReturn403ForAnotherPlayer() throws Exception {
        UUID matchId = UUID.randomUUID();
        when(matchService.startNextRound(matchId, "player@email.com"))
                .thenThrow(new ForbiddenOperationException("Apenas o criador pode abrir a rodada"));
        // A regra do service vira uma resposta de acesso negado.
        mockMvc.perform(post("/api/matches/{matchId}/rounds/next", matchId)
                        .principal(new UsernamePasswordAuthenticationToken("player@email.com", null)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn409ForAnOpenRound() throws Exception {
        UUID matchId = UUID.randomUUID();
        when(matchService.startNextRound(matchId, "owner@email.com"))
                .thenThrow(new ResourceConflictException("Já existe uma rodada aberta"));
        mockMvc.perform(post("/api/matches/{matchId}/rounds/next", matchId)
                        .principal(new UsernamePasswordAuthenticationToken("owner@email.com", null)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn404ForMissingMatch() throws Exception {
        UUID matchId = UUID.randomUUID();
        when(matchService.startNextRound(matchId, "owner@email.com"))
                .thenThrow(new EntityNotFoundException("Partida não encontrada"));
        mockMvc.perform(post("/api/matches/{matchId}/rounds/next", matchId)
                        .principal(new UsernamePasswordAuthenticationToken("owner@email.com", null)))
                .andExpect(status().isNotFound());
    }
}
