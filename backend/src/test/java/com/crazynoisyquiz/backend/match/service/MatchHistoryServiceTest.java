package com.crazynoisyquiz.backend.match.service;

import com.crazynoisyquiz.backend.match.model.Match;
import com.crazynoisyquiz.backend.match.model.MatchStatus;
import com.crazynoisyquiz.backend.match.repository.MatchRepository;
import com.crazynoisyquiz.backend.room.model.QuizRoom;
import com.crazynoisyquiz.backend.room.repository.QuizRoomRepository;
import com.crazynoisyquiz.backend.room.repository.RoomParticipantRepository;
import com.crazynoisyquiz.backend.shared.exception.ForbiddenOperationException;
import com.crazynoisyquiz.backend.shared.exception.InvalidRequestException;
import com.crazynoisyquiz.backend.user.model.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchHistoryServiceTest {
    @Mock private MatchRepository matches;
    @Mock private QuizRoomRepository rooms;
    @Mock private RoomParticipantRepository participants;
    @InjectMocks private MatchService service;
    private QuizRoom room;

    @BeforeEach
    void setUp() {
        room = QuizRoom.builder().id(UUID.randomUUID()).code("ABC123")
                .owner(User.builder().email("owner@email.com").build()).build();
    }

    private void existingRoom() {
        when(rooms.findByCode("ABC123")).thenReturn(Optional.of(room));
    }

    @Test
    void shouldReturnPagedHistoryForOwnerWithStableDescendingOrder() {
        existingRoom();
        var pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Match match = Match.builder().id(UUID.randomUUID()).status(MatchStatus.FINISHED)
                .totalRounds(5).currentRoundNumber(5).createdAt(Instant.now())
                .startedAt(Instant.now()).finishedAt(Instant.now()).build();
        when(matches.findAllByRoomId(room.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(match), pageable, 3));
        // O histórico mantém os horários e informa a quantidade de páginas.
        var result = service.findHistory("ABC123", "owner@email.com", 1, 2);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.content().getFirst().matchId()).isEqualTo(match.getId());
        assertThat(result.content().getFirst().status()).isEqualTo(MatchStatus.FINISHED);
        assertThat(result.content().getFirst().finishedAt()).isEqualTo(match.getFinishedAt());
        verifyNoInteractions(participants);
    }

    @Test
    void shouldAllowActiveParticipantAndReturnEmptyHistory() {
        existingRoom();
        when(participants.existsByRoomIdAndUserEmailIgnoreCaseAndLeftAtIsNull(room.getId(), "player@email.com"))
                .thenReturn(true);
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        when(matches.findAllByRoomId(room.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));
        assertThat(service.findHistory("ABC123", "player@email.com", 0, 10).content()).isEmpty();
    }

    @Test
    void shouldRejectOutsiderOrFormerParticipant() {
        existingRoom();
        assertThatThrownBy(() -> service.findHistory("ABC123", "outsider@email.com", 0, 10))
                .isInstanceOf(ForbiddenOperationException.class);
        verifyNoInteractions(matches);
    }

    @Test
    void shouldRejectMissingRoom() {
        when(rooms.findByCode("ABC123")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findHistory("ABC123", "owner@email.com", 0, 10))
                .isInstanceOf(EntityNotFoundException.class);
        verifyNoInteractions(matches);
    }

    @ParameterizedTest
    @CsvSource({"-1,10", "0,0", "0,-1", "0,51"})
    void shouldRejectInvalidPagination(int page, int size) {
        // Limitar a página evita consultas desnecessariamente grandes.
        assertThatThrownBy(() -> service.findHistory("ABC123", "owner@email.com", page, size))
                .isInstanceOf(InvalidRequestException.class);
        verifyNoInteractions(rooms, matches, participants);
    }
}
