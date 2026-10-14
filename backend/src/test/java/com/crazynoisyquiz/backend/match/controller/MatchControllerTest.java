package com.crazynoisyquiz.backend.match.controller;

import com.crazynoisyquiz.backend.auth.service.JwtService;
import com.crazynoisyquiz.backend.match.dto.MatchResponse;
import com.crazynoisyquiz.backend.match.dto.MatchHistoryPageResponse;
import com.crazynoisyquiz.backend.match.dto.MatchHistoryResponse;
import com.crazynoisyquiz.backend.match.model.MatchStatus;
import com.crazynoisyquiz.backend.match.service.MatchService;
import com.crazynoisyquiz.backend.shared.exception.ForbiddenOperationException;
import com.crazynoisyquiz.backend.shared.exception.GlobalExceptionHandler;
import com.crazynoisyquiz.backend.shared.exception.InvalidRequestException;
import com.crazynoisyquiz.backend.shared.exception.ResourceConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MatchController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MatchControllerTest {

    @Test
    void shouldReturnHistoryWithDefaultPagination() throws Exception {
        UUID matchId = UUID.randomUUID();
        when(matchService.findHistory("ABC123", "owner@email.com", 0, 10))
                .thenReturn(new MatchHistoryPageResponse(List.of(new MatchHistoryResponse(
                        matchId, MatchStatus.IN_PROGRESS, 5, 0, null, null, null
                )), 0, 10, 1, 1));
        // O GET usa a mesma sala da URL e o usuário autenticado.
        mockMvc.perform(get("/api/rooms/ABC123/matches")
                        .principal(new UsernamePasswordAuthenticationToken("owner@email.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].matchId").value(matchId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));
        verify(matchService).findHistory("ABC123", "owner@email.com", 0, 10);
    }

    @Test
    void shouldAcceptCustomHistoryPage() throws Exception {
        when(matchService.findHistory("ABC123", "owner@email.com", 2, 5))
                .thenReturn(new MatchHistoryPageResponse(List.of(), 2, 5, 0, 0));
        mockMvc.perform(get("/api/rooms/ABC123/matches").param("page", "2").param("size", "5")
                        .principal(new UsernamePasswordAuthenticationToken("owner@email.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2));
        verify(matchService).findHistory("ABC123", "owner@email.com", 2, 5);
    }

    @Test
    void shouldRejectInvalidHistoryPagination() throws Exception {
        when(matchService.findHistory("ABC123", "owner@email.com", -1, 10))
                .thenThrow(new InvalidRequestException("Página inválida"));
        mockMvc.perform(get("/api/rooms/ABC123/matches").param("page", "-1")
                        .principal(new UsernamePasswordAuthenticationToken("owner@email.com", null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectHistoryAccessForOutsider() throws Exception {
        when(matchService.findHistory("ABC123", "outsider@email.com", 0, 10))
                .thenThrow(new ForbiddenOperationException("Acesso negado ao histórico"));
        mockMvc.perform(get("/api/rooms/ABC123/matches")
                        .principal(new UsernamePasswordAuthenticationToken("outsider@email.com", null)))
                .andExpect(status().isForbidden());
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatchService matchService;

    // O filtro JWT é desativado neste teste, mas sua dependência ainda faz parte do contexto.
    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldStartMatchAndReturn201() throws Exception {
        UUID matchId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(matchService.startMatch(eq("ABC123"), eq("owner@email.com"), any()))
                .thenReturn(new MatchResponse(
                        matchId, "ABC123", MatchStatus.IN_PROGRESS, 5, 0, List.of(categoryId)
                ));

        // O endpoint aceita as escolhas do criador e retorna o resumo da partida criada.
        mockMvc.perform(post("/api/rooms/ABC123/matches")
                        .principal(new UsernamePasswordAuthenticationToken(
                                "owner@email.com", null
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryIds":["%s"],"totalRounds":5}
                                """.formatted(categoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(matchId.toString()))
                .andExpect(jsonPath("$.roomCode").value("ABC123"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.totalRounds").value(5))
                .andExpect(jsonPath("$.currentRoundNumber").value(0))
                .andExpect(jsonPath("$.categoryIds[0]").value(categoryId.toString()));

        verify(matchService).startMatch(eq("ABC123"), eq("owner@email.com"), any());
    }

    @Test
    void shouldReturn400WhenRequestViolatesRoundOrCategoryValidation() throws Exception {
        // A validação do DTO barra a requisição antes de chegar ao serviço.
        mockMvc.perform(post("/api/rooms/ABC123/matches")
                        .principal(new UsernamePasswordAuthenticationToken(
                                "owner@email.com", null
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryIds":[],"totalRounds":4}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturn403WhenCallerIsNotRoomOwner() throws Exception {
        UUID categoryId = UUID.randomUUID();
        when(matchService.startMatch(eq("ABC123"), eq("player@email.com"), any()))
                .thenThrow(new ForbiddenOperationException(
                        "Apenas o criador da sala pode iniciar a partida"
                ));

        mockMvc.perform(post("/api/rooms/ABC123/matches")
                        .principal(new UsernamePasswordAuthenticationToken(
                                "player@email.com", null
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryIds":["%s"],"totalRounds":5}
                                """.formatted(categoryId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Apenas o criador da sala pode iniciar a partida"));
    }

    @Test
    void shouldReturn400WhenSelectedCategoriesDoNotHaveEnoughQuestions() throws Exception {
        UUID categoryId = UUID.randomUUID();
        when(matchService.startMatch(eq("ABC123"), eq("owner@email.com"), any()))
                .thenThrow(new InvalidRequestException(
                        "As categorias escolhidas não têm perguntas suficientes para essa partida"
                ));

        mockMvc.perform(post("/api/rooms/ABC123/matches")
                        .principal(new UsernamePasswordAuthenticationToken(
                                "owner@email.com", null
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryIds":["%s"],"totalRounds":5}
                                """.formatted(categoryId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("As categorias escolhidas não têm perguntas suficientes para essa partida"));
    }

    @Test
    void shouldReturn409WhenRoomIsNotWaitingForAMatch() throws Exception {
        UUID categoryId = UUID.randomUUID();
        when(matchService.startMatch(eq("ABC123"), eq("owner@email.com"), any()))
                .thenThrow(new ResourceConflictException(
                        "A sala não está aguardando uma partida"
                ));

        mockMvc.perform(post("/api/rooms/ABC123/matches")
                        .principal(new UsernamePasswordAuthenticationToken(
                                "owner@email.com", null
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryIds":["%s"],"totalRounds":5}
                                """.formatted(categoryId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("A sala não está aguardando uma partida"));
    }
}
