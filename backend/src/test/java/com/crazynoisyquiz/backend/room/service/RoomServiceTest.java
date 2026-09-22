package com.crazynoisyquiz.backend.room.service;

import com.crazynoisyquiz.backend.room.dto.CreateRoomRequest;
import com.crazynoisyquiz.backend.room.dto.RoomResponse;
import com.crazynoisyquiz.backend.room.model.QuizRoom;
import com.crazynoisyquiz.backend.room.model.RoomParticipant;
import com.crazynoisyquiz.backend.room.model.RoomStatus;
import com.crazynoisyquiz.backend.room.repository.QuizRoomRepository;
import com.crazynoisyquiz.backend.room.repository.RoomParticipantRepository;
import com.crazynoisyquiz.backend.user.model.User;
import com.crazynoisyquiz.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private QuizRoomRepository quizRoomRepository;

    @Mock
    private RoomParticipantRepository roomParticipantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoomService roomService;

    @Test
    void shouldCreateRoomWithOwnerAndDefaultValues() {
        User owner = User.builder()
                .id(UUID.randomUUID())
                .email("gil@email.com")
                .username("gil")
                .build();

        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(quizRoomRepository.existsByCode(any())).thenReturn(false);
        when(quizRoomRepository.save(any(QuizRoom.class))).thenAnswer(invocation -> {
            QuizRoom room = invocation.getArgument(0);
            room.setId(UUID.randomUUID());
            room.setCreatedAt(Instant.now());
            return room;
        });

        RoomResponse response = roomService.create(null, owner.getEmail());

        assertThat(response.getCode()).hasSize(6);
        assertThat(response.getOwnerId()).isEqualTo(owner.getId());
        assertThat(response.getStatus()).isEqualTo(RoomStatus.WAITING);
        assertThat(response.getMaxPlayers()).isEqualTo(8);
        verify(quizRoomRepository).save(any(QuizRoom.class));
        ArgumentCaptor<RoomParticipant> participationCaptor =
                ArgumentCaptor.forClass(RoomParticipant.class);
        verify(roomParticipantRepository).save(participationCaptor.capture());
        assertThat(participationCaptor.getValue().getRoom().getId())
                .isEqualTo(response.getId());
        assertThat(participationCaptor.getValue().getUser()).isEqualTo(owner);
        assertThat(participationCaptor.getValue().getJoinedAt()).isNotNull();
    }

    @Test
    void shouldCreateRoomWithRequestedPlayerLimit() {
        User owner = User.builder()
                .id(UUID.randomUUID())
                .email("gil@email.com")
                .build();
        CreateRoomRequest request = new CreateRoomRequest();
        request.setMaxPlayers(4);

        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(quizRoomRepository.existsByCode(any())).thenReturn(false);
        when(quizRoomRepository.save(any(QuizRoom.class)))
                .thenAnswer(invocation -> {
                    QuizRoom room = invocation.getArgument(0);
                    room.setId(UUID.randomUUID());
                    room.setCreatedAt(Instant.now());
                    return room;
                });

        RoomResponse response = roomService.create(request, owner.getEmail());

        assertThat(response.getMaxPlayers()).isEqualTo(4);
    }
}
