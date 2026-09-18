package com.crazynoisyquiz.backend.room.service;

import com.crazynoisyquiz.backend.room.dto.RoomParticipantResponse;
import com.crazynoisyquiz.backend.room.model.QuizRoom;
import com.crazynoisyquiz.backend.room.model.RoomParticipant;
import com.crazynoisyquiz.backend.room.model.RoomStatus;
import com.crazynoisyquiz.backend.room.repository.QuizRoomRepository;
import com.crazynoisyquiz.backend.room.repository.RoomParticipantRepository;
import com.crazynoisyquiz.backend.user.model.User;
import com.crazynoisyquiz.backend.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        when(roomParticipantRepository.existsByRoomIdAndUserId(roomId, userId))
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
        when(roomParticipantRepository.existsByRoomIdAndUserId(roomId, userId))
                .thenReturn(true);

        assertThatThrownBy(() -> roomParticipantService.joinRoom(
                "R3XCD5",
                user.getEmail()
        ))
                .isInstanceOf(IllegalStateException.class)
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
        when(roomParticipantRepository.existsByRoomIdAndUserId(roomId, userId))
                .thenReturn(false);
        when(roomParticipantRepository.countByRoomIdAndLeftAtIsNull(roomId))
                .thenReturn(2L);

        assertThatThrownBy(() -> roomParticipantService.joinRoom(
                "R3XCD5",
                user.getEmail()
        ))
                .isInstanceOf(IllegalStateException.class)
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
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Não é possível entrar em uma sala que já foi iniciada");

        verify(userRepository, never()).findByEmail(any());
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
