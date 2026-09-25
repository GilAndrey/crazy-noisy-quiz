package com.crazynoisyquiz.backend.room.controller;

import com.crazynoisyquiz.backend.auth.service.JwtService;
import com.crazynoisyquiz.backend.room.dto.RoomParticipantResponse;
import com.crazynoisyquiz.backend.room.dto.RoomResponse;
import com.crazynoisyquiz.backend.room.model.RoomStatus;
import com.crazynoisyquiz.backend.room.service.RoomService;
import com.crazynoisyquiz.backend.shared.exception.GlobalExceptionHandler;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomService roomService;

    // O filtro JWT é criado no contexto, mas não é o foco deste teste.
    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldCreateRoomAndReturn201() throws Exception {
        UUID roomId = UUID.randomUUID();
        RoomResponse response = RoomResponse.builder()
                .id(roomId)
                .code("24JYMZ")
                .ownerId(UUID.randomUUID())
                .status(RoomStatus.WAITING)
                .maxPlayers(8)
                .createdAt(Instant.now())
                .build();

        when(roomService.create(any(), any())).thenReturn(response);

        // O controller deve repassar a criação bem-sucedida com status 201.
        mockMvc.perform(post("/api/rooms")
                        .principal(new UsernamePasswordAuthenticationToken(
                                "gil@email.com",
                                null
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxPlayers\":8}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(roomId.toString()))
                .andExpect(jsonPath("$.code").value("24JYMZ"))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.maxPlayers").value(8));
    }

    @Test
    void shouldReturn400WhenPlayerLimitIsInvalid() throws Exception {
        // A validação do DTO bloqueia limites fora das regras antes do service ser chamado.
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxPlayers\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.maxPlayers").exists());
    }

    @Test
    void shouldFindRoomByCodeAndReturn200() throws Exception {
        UUID roomId = UUID.randomUUID();
        RoomParticipantResponse participant = RoomParticipantResponse.builder()
                .id(UUID.randomUUID())
                .roomId(roomId)
                .userId(UUID.randomUUID())
                .roomCode("19UIP7")
                .joinedAt(Instant.now())
                .build();
        RoomResponse response = RoomResponse.builder()
                .id(roomId)
                .code("19UIP7")
                .ownerId(UUID.randomUUID())
                .status(RoomStatus.WAITING)
                .maxPlayers(8)
                .createdAt(Instant.now())
                .participants(List.of(participant))
                .build();

        when(roomService.findByCode("19UIP7")).thenReturn(response);

        // O endpoint deve localizar a sala pelo código e responder com HTTP 200.
        mockMvc.perform(get("/api/rooms/19UIP7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(roomId.toString()))
                .andExpect(jsonPath("$.code").value("19UIP7"))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.maxPlayers").value(8))
                .andExpect(jsonPath("$.participants.length()").value(1))
                .andExpect(jsonPath("$.participants[0].userId")
                        .value(participant.getUserId().toString()));
    }

    @Test
    void shouldReturn404WhenRoomCodeDoesNotExist() throws Exception {
        when(roomService.findByCode("NOEXIST"))
                .thenThrow(new EntityNotFoundException("Sala não encontrada"));

        mockMvc.perform(get("/api/rooms/NOEXIST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Sala não encontrada"));
    }
}
