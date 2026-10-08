package com.crazynoisyquiz.backend.match.controller;

import com.crazynoisyquiz.backend.auth.service.JwtService;
import com.crazynoisyquiz.backend.match.dto.SubmitAnswerRequest;
import com.crazynoisyquiz.backend.match.dto.SubmitAnswerResponse;
import com.crazynoisyquiz.backend.match.service.MatchAnswerService;
import com.crazynoisyquiz.backend.shared.exception.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MatchAnswerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MatchAnswerControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private MatchAnswerService service;
    @MockitoBean private JwtService jwtService;
    private final UUID matchId = UUID.randomUUID();
    private final UUID roundId = UUID.randomUUID();
    private final UUID optionId = UUID.randomUUID();

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request(String body) {
        // Como os filtros estão desativados, passamos o usuário diretamente na requisição.
        return post("/api/matches/{matchId}/rounds/{roundId}/answers", matchId, roundId)
                .principal(new UsernamePasswordAuthenticationToken("player@email.com", null))
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void shouldReturn201WithoutRevealingResult() throws Exception {
        UUID answerId = UUID.randomUUID();
        Instant answeredAt = Instant.parse("2026-10-07T12:00:00Z");
        when(service.submitAnswer(matchId, roundId, "player@email.com", new SubmitAnswerRequest(optionId)))
                .thenReturn(new SubmitAnswerResponse(answerId, roundId, optionId, answeredAt));
        mockMvc.perform(request("{\"optionId\":\"%s\"}".formatted(optionId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(answerId.toString()))
                .andExpect(jsonPath("$.roundId").value(roundId.toString()))
                .andExpect(jsonPath("$.optionId").value(optionId.toString()))
                .andExpect(jsonPath("$.answeredAt").value(answeredAt.toString()))
                .andExpect(jsonPath("$.correct").doesNotExist())
                .andExpect(jsonPath("$.isCorrect").doesNotExist())
                .andExpect(jsonPath("$.points").doesNotExist());
        verify(service).submitAnswer(matchId, roundId, "player@email.com", new SubmitAnswerRequest(optionId));
    }

    @Test
    void shouldRejectMissingOptionBeforeCallingService() throws Exception {
        mockMvc.perform(request("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.optionId").value("Selecione uma alternativa"));
        verifyNoInteractions(service);
    }

    private void failure(RuntimeException error, int expectedStatus) throws Exception {
        when(service.submitAnswer(matchId, roundId, "player@email.com", new SubmitAnswerRequest(optionId)))
                .thenThrow(error);
        mockMvc.perform(request("{\"optionId\":\"%s\"}".formatted(optionId)))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.message").value(error.getMessage()));
    }

    @Test
    void shouldReturn403ForNonParticipant() throws Exception {
        failure(new ForbiddenOperationException("Apenas participantes podem responder"), 403);
    }

    @Test
    void shouldReturn404ForMissingRound() throws Exception {
        failure(new EntityNotFoundException("Rodada não encontrada"), 404);
    }

    @Test
    void shouldReturn400ForUnrelatedAlternative() throws Exception {
        failure(new InvalidRequestException("A alternativa não pertence à pergunta dessa rodada"), 400);
    }

    @Test
    void shouldReturn409ForDuplicateAnswer() throws Exception {
        failure(new ResourceConflictException("Você já respondeu essa rodada"), 409);
    }

    @Test
    void shouldReturn409ForExpiredAnswer() throws Exception {
        failure(new ResourceConflictException("O tempo para responder essa rodada acabou"), 409);
    }
}
