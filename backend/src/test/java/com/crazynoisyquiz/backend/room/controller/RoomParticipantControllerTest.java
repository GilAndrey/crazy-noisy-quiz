package com.crazynoisyquiz.backend.room.controller;

import com.crazynoisyquiz.backend.auth.service.JwtService;
import com.crazynoisyquiz.backend.room.dto.RoomParticipantResponse;
import com.crazynoisyquiz.backend.room.service.RoomParticipantService;
import com.crazynoisyquiz.backend.shared.exception.GlobalExceptionHandler;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomParticipantController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RoomParticipantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomParticipantService roomParticipantService;

    // O filtro JWT é criado no contexto, mas não é o foco deste teste.
    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldJoinRoomAndReturn201() throws Exception {
        UUID participantId = UUID.randomUUID();
        RoomParticipantResponse response = RoomParticipantResponse.builder()
                .id(participantId)
                .roomId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .roomCode("R3XCD5")
                .joinedAt(Instant.now())
                .build();

        when(roomParticipantService.joinRoom(any(), any())).thenReturn(response);

        // O usuário autenticado é enviado diretamente na requisição simulada.
        mockMvc.perform(post("/api/rooms/R3XCD5/join")
                        .principal(new UsernamePasswordAuthenticationToken(
                                "gil@email.com",
                                null
                        ))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(participantId.toString()))
                .andExpect(jsonPath("$.roomCode").value("R3XCD5"))
                .andExpect(jsonPath("$.joinedAt").exists());
    }

    @Test
    void shouldLeaveRoomAndReturn204() throws Exception {
        doNothing().when(roomParticipantService).leaveRoom("R3XCD5", "gil@email.com");

        // A saída não precisa de corpo na resposta; 204 confirma que foi processada.
        mockMvc.perform(delete("/api/rooms/R3XCD5/leave")
                        .principal(new UsernamePasswordAuthenticationToken(
                                "gil@email.com",
                                null
                        )))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldListActiveParticipantsAndReturn200() throws Exception {
        RoomParticipantResponse participant = RoomParticipantResponse.builder()
                .id(UUID.randomUUID())
                .roomId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .roomCode("R3XCD5")
                .joinedAt(Instant.now())
                .build();

        when(roomParticipantService.findActiveParticipants("R3XCD5"))
                .thenReturn(List.of(participant));

        // A listagem deve devolver os participantes ativos no formato de lista JSON.
        mockMvc.perform(get("/api/rooms/R3XCD5/participants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(participant.getId().toString()))
                .andExpect(jsonPath("$[0].roomCode").value("R3XCD5"));
    }
}
