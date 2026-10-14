package com.crazynoisyquiz.backend.room.service;

import com.crazynoisyquiz.backend.match.model.Match;
import com.crazynoisyquiz.backend.match.model.MatchStatus;
import com.crazynoisyquiz.backend.match.repository.MatchRepository;
import com.crazynoisyquiz.backend.room.model.QuizRoom;
import com.crazynoisyquiz.backend.room.model.RoomStatus;
import com.crazynoisyquiz.backend.room.repository.QuizRoomRepository;
import com.crazynoisyquiz.backend.room.repository.RoomParticipantRepository;
import com.crazynoisyquiz.backend.shared.exception.ForbiddenOperationException;
import com.crazynoisyquiz.backend.shared.exception.ResourceConflictException;
import com.crazynoisyquiz.backend.user.model.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomCloseServiceTest {
    @Mock private QuizRoomRepository rooms;
    @Mock private RoomParticipantRepository participants;
    @Mock private MatchRepository matches;
    @InjectMocks private RoomService service;
    private QuizRoom room;

    @BeforeEach
    void setUp() {
        room = QuizRoom.builder().id(UUID.randomUUID()).code("ABC123").status(RoomStatus.WAITING)
                .owner(User.builder().id(UUID.randomUUID()).email("owner@email.com").build()).build();
    }

    private void existingRoom() {
        when(rooms.findByCodeForUpdate("ABC123")).thenReturn(Optional.of(room));
    }

    @Test
    void shouldCloseWaitingRoomAndKeepItsRecords() {
        existingRoom();
        var response = service.closeRoom("ABC123", "owner@email.com");
        assertThat(response.getStatus()).isEqualTo(RoomStatus.CLOSED);
        assertThat(response.getClosedAt()).isNotNull().isEqualTo(room.getClosedAt());
        // Fechar não apaga o histórico nem remove os participantes.
        verify(rooms, never()).delete(any());
        verify(matches, never()).delete(any());
        verify(participants, never()).delete(any());
    }

    @Test
    void shouldCancelActiveMatchBeforeClosingRoom() {
        existingRoom();
        room.setStatus(RoomStatus.IN_PROGRESS);
        Match active = Match.builder().id(UUID.randomUUID()).room(room).status(MatchStatus.IN_PROGRESS)
                .currentRoundNumber(2).build();
        when(matches.findByRoomCodeAndStatusForUpdate("ABC123", MatchStatus.IN_PROGRESS))
                .thenReturn(List.of(active));
        service.closeRoom("ABC123", "owner@email.com");
        assertThat(active.getStatus()).isEqualTo(MatchStatus.CANCELLED);
        assertThat(active.getFinishedAt()).isEqualTo(room.getClosedAt());
        assertThat(active.getCurrentRoundNumber()).isEqualTo(2);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.CLOSED);
        // Mantém a ordem dos bloqueios usada quando a última rodada termina.
        var order = inOrder(matches, rooms);
        order.verify(matches).findByRoomCodeAndStatusForUpdate("ABC123", MatchStatus.IN_PROGRESS);
        order.verify(rooms).findByCodeForUpdate("ABC123");
    }

    @Test
    void shouldRejectAnotherPlayerWithoutChangingRoomOrMatch() {
        existingRoom();
        Match active = Match.builder().status(MatchStatus.IN_PROGRESS).build();
        when(matches.findByRoomCodeAndStatusForUpdate("ABC123", MatchStatus.IN_PROGRESS))
                .thenReturn(List.of(active));
        assertThatThrownBy(() -> service.closeRoom("ABC123", "player@email.com"))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.WAITING);
        assertThat(room.getClosedAt()).isNull();
        assertThat(active.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
    }

    @Test
    void shouldRejectAlreadyClosedRoom() {
        existingRoom();
        room.setStatus(RoomStatus.CLOSED);
        assertThatThrownBy(() -> service.closeRoom("ABC123", "owner@email.com"))
                .isInstanceOf(ResourceConflictException.class).hasMessage("A sala já está fechada");
    }

    @Test
    void shouldRejectMissingRoom() {
        when(rooms.findByCodeForUpdate("ABC123")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.closeRoom("ABC123", "owner@email.com"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldRequireRetryIfMatchStartedBetweenLockQueries() {
        existingRoom();
        when(matches.existsByRoomIdAndStatus(room.getId(), MatchStatus.IN_PROGRESS)).thenReturn(true);
        // Não fechamos uma sala deixando para trás uma partida nova sem bloqueio.
        assertThatThrownBy(() -> service.closeRoom("ABC123", "owner@email.com"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Uma partida acabou de iniciar; tente encerrar a sala novamente");
        assertThat(room.getStatus()).isEqualTo(RoomStatus.WAITING);
        assertThat(room.getClosedAt()).isNull();
    }
}
