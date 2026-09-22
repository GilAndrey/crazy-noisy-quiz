package com.crazynoisyquiz.backend.room.service;

import com.crazynoisyquiz.backend.room.dto.RoomParticipantResponse;
import com.crazynoisyquiz.backend.room.model.QuizRoom;
import com.crazynoisyquiz.backend.room.model.RoomParticipant;
import com.crazynoisyquiz.backend.room.model.RoomStatus;
import com.crazynoisyquiz.backend.room.repository.QuizRoomRepository;
import com.crazynoisyquiz.backend.room.repository.RoomParticipantRepository;
import com.crazynoisyquiz.backend.shared.exception.ResourceConflictException;
import com.crazynoisyquiz.backend.user.model.User;
import com.crazynoisyquiz.backend.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomParticipantServiceTest {

    @Mock
    private QuizRoomRepository quizRoomRepository;

    @Mock
    private RoomParticipantRepository roomParticipantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoomParticipantService roomParticipantService;

    @Test
    void shouldJoinRoomSuccessfully() {
        UUID roomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        QuizRoom room = createRoom(roomId, RoomStatus.WAITING, 8);
        User user = createUser(userId, "gil@email.com");

        when(quizRoomRepository.findByCode("R3XCD5")).thenReturn(Optional.of(room));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(roomParticipantRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(roomId, userId))
                .thenReturn(false);
        when(roomParticipantRepository.countByRoomIdAndLeftAtIsNull(roomId))
                .thenReturn(0L);
        when(roomParticipantRepository.save(any(RoomParticipant.class)))
                .thenAnswer(invocation -> {
                    RoomParticipant participant = invocation.getArgument(0);
                    participant.setId(UUID.randomUUID());
                    return participant;
                });

        RoomParticipantResponse response = roomParticipantService.joinRoom(
                "R3XCD5",
                user.getEmail()
        );

        assertThat(response.getRoomId()).isEqualTo(roomId);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getRoomCode()).isEqualTo("R3XCD5");
        assertThat(response.getJoinedAt()).isNotNull();
        verify(roomParticipantRepository).save(any(RoomParticipant.class));
    }

    @Test
    void shouldRejectWhenUserIsAlreadyInRoom() {
        UUID roomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        QuizRoom room = createRoom(roomId, RoomStatus.WAITING, 8);
        User user = createUser(userId, "gil@email.com");

        when(quizRoomRepository.findByCode("R3XCD5")).thenReturn(Optional.of(room));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(roomParticipantRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(roomId, userId))
                .thenReturn(true);

        assertThatThrownBy(() -> roomParticipantService.joinRoom(
                "R3XCD5",
                user.getEmail()
        ))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Usuário já está participando dessa sala");

        verify(roomParticipantRepository, never()).save(any());
    }

    @Test
    void shouldRejectWhenRoomIsFull() {
        UUID roomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        QuizRoom room = createRoom(roomId, RoomStatus.WAITING, 2);
        User user = createUser(userId, "gil@email.com");

        when(quizRoomRepository.findByCode("R3XCD5")).thenReturn(Optional.of(room));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(roomParticipantRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(roomId, userId))
                .thenReturn(false);
        when(roomParticipantRepository.countByRoomIdAndLeftAtIsNull(roomId))
                .thenReturn(2L);

        assertThatThrownBy(() -> roomParticipantService.joinRoom(
                "R3XCD5",
                user.getEmail()
        ))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("A sala está cheia");

        verify(roomParticipantRepository, never()).save(any());
    }

    @Test
    void shouldRejectWhenRoomIsNotWaiting() {
        UUID roomId = UUID.randomUUID();
        QuizRoom room = createRoom(roomId, RoomStatus.IN_PROGRESS, 8);

        when(quizRoomRepository.findByCode("R3XCD5")).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> roomParticipantService.joinRoom(
                "R3XCD5",
                "gil@email.com"
        ))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Não é possível entrar em uma sala que já foi iniciada");

        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void shouldLeaveRoomAndSetLeftAt() {
        // Montamos uma participação ativa, como ela estaria antes do jogador sair.
        UUID roomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        QuizRoom room = createRoom(roomId, RoomStatus.WAITING, 8);
        User user = createUser(userId, "gil@email.com");
        RoomParticipant participant = new RoomParticipant();
        participant.setRoom(room);
        participant.setUser(user);
        participant.setJoinedAt(java.time.Instant.now());

        when(quizRoomRepository.findByCode("R3XCD5")).thenReturn(Optional.of(room));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(roomParticipantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(roomId, userId))
                .thenReturn(Optional.of(participant));

        // A saída não apaga o registro; apenas preenche o horário de saída.
        roomParticipantService.leaveRoom("R3XCD5", user.getEmail());

        assertThat(participant.getLeftAt()).isNotNull();
        verify(roomParticipantRepository).save(participant);
    }

    @Test
    void shouldRejectLeavingWhenUserIsNotInRoom() {
        // Simulamos um usuário cadastrado que não possui participação ativa na sala.
        UUID roomId = UUID.randomUUID();
        QuizRoom room = createRoom(roomId, RoomStatus.WAITING, 8);
        User user = createUser(UUID.randomUUID(), "gil@email.com");

        when(quizRoomRepository.findByCode("R3XCD5")).thenReturn(Optional.of(room));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(roomParticipantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(
                roomId,
                user.getId()
        )).thenReturn(Optional.empty());

        // Sem participação ativa, a saída deve ser recusada e nada deve ser salvo.
        assertThatThrownBy(() -> roomParticipantService.leaveRoom(
                "R3XCD5",
                user.getEmail()
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Usuário não está participando desta sala");

        verify(roomParticipantRepository, never()).save(any());
    }

    @Test
    void shouldReturnOnlyActiveParticipantsForRoom() {
        UUID roomId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        QuizRoom room = createRoom(roomId, RoomStatus.WAITING, 8);
        User user = createUser(userId, "gil@email.com");
        RoomParticipant activeParticipant = new RoomParticipant();
        activeParticipant.setId(participantId);
        activeParticipant.setRoom(room);
        activeParticipant.setUser(user);
        activeParticipant.setJoinedAt(Instant.now());

        when(quizRoomRepository.findByCode("R3XCD5")).thenReturn(Optional.of(room));
        when(roomParticipantRepository.findAllByRoomIdAndLeftAtIsNull(roomId))
                .thenReturn(List.of(activeParticipant));

        // O repository já filtra quem saiu; o service transforma os registros em DTOs.
        List<RoomParticipantResponse> participants =
                roomParticipantService.findActiveParticipants("R3XCD5");

        assertThat(participants).hasSize(1);
        assertThat(participants.get(0).getId()).isEqualTo(participantId);
        assertThat(participants.get(0).getUserId()).isEqualTo(userId);
        assertThat(participants.get(0).getRoomCode()).isEqualTo("R3XCD5");
    }

    @Test
    void shouldReturnEmptyListWhenRoomHasNoActiveParticipants() {
        UUID roomId = UUID.randomUUID();
        QuizRoom room = createRoom(roomId, RoomStatus.WAITING, 8);

        when(quizRoomRepository.findByCode("R3XCD5")).thenReturn(Optional.of(room));
        when(roomParticipantRepository.findAllByRoomIdAndLeftAtIsNull(roomId))
                .thenReturn(List.of());

        // Uma sala sem jogadores ativos é uma resposta válida, então retornamos lista vazia.
        assertThat(roomParticipantService.findActiveParticipants("R3XCD5")).isEmpty();
    }

    @Test
    void shouldFailToListParticipantsWhenRoomDoesNotExist() {
        when(quizRoomRepository.findByCode("NOEXIST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomParticipantService.findActiveParticipants("NOEXIST"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Sala não encontrada");

        verify(roomParticipantRepository, never()).findAllByRoomIdAndLeftAtIsNull(any());
    }

    private QuizRoom createRoom(UUID id, RoomStatus status, int maxPlayers) {
        return QuizRoom.builder()
                .id(id)
                .code("R3XCD5")
                .status(status)
                .maxPlayers(maxPlayers)
                .build();
    }

    private User createUser(UUID id, String email) {
        return User.builder()
                .id(id)
                .email(email)
                .username("gil")
                .build();
    }
}
