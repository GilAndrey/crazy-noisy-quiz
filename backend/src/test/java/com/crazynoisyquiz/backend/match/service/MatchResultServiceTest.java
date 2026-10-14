package com.crazynoisyquiz.backend.match.service;

import com.crazynoisyquiz.backend.match.model.Match;
import com.crazynoisyquiz.backend.match.model.MatchParticipant;
import com.crazynoisyquiz.backend.match.model.MatchStatus;
import com.crazynoisyquiz.backend.match.repository.MatchParticipantRepository;
import com.crazynoisyquiz.backend.match.repository.MatchRepository;
import com.crazynoisyquiz.backend.room.model.QuizRoom;
import com.crazynoisyquiz.backend.shared.exception.ForbiddenOperationException;
import com.crazynoisyquiz.backend.shared.exception.ResourceConflictException;
import com.crazynoisyquiz.backend.user.model.AvatarKey;
import com.crazynoisyquiz.backend.user.model.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchResultServiceTest {
    @Mock private MatchRepository matches;
    @Mock private MatchParticipantRepository participants;
    @InjectMocks private MatchService service;
    private Match match;

    @BeforeEach
    void setUp() {
        match = Match.builder().id(UUID.randomUUID())
                .room(QuizRoom.builder().code("ABC123").build())
                .status(MatchStatus.FINISHED).totalRounds(5)
                .finishedAt(Instant.parse("2026-10-14T15:12:26Z")).build();
    }

    private void participatingPlayer() {
        when(matches.findById(match.getId())).thenReturn(Optional.of(match));
        when(participants.existsByMatchIdAndUserEmailIgnoreCase(match.getId(), "gil@email.com"))
                .thenReturn(true);
    }

    private MatchParticipant player(String username, int position, int points, int correct) {
        User user = User.builder().id(UUID.randomUUID()).username(username)
                .avatarKey(AvatarKey.AVATAR_01).build();
        return MatchParticipant.builder().match(match).user(user).position(position)
                .totalPoints(points).correctAnswers(correct).build();
    }

    @Test
    void shouldReturnSavedRankingWithoutChangingScoresOrPositions() {
        participatingPlayer();
        MatchParticipant first = player("gil", 1, 200, 3);
        MatchParticipant tied = player("labubu", 1, 200, 3);
        MatchParticipant third = player("outro", 3, 200, 2);
        when(participants.findAllByMatchIdOrderByTotalPointsDescCorrectAnswersDesc(match.getId()))
                .thenReturn(List.of(first, tied, third));

        // A consulta preserva o empate e as posições calculadas no encerramento.
        var result = service.findResult(match.getId(), "gil@email.com");
        assertThat(result.matchId()).isEqualTo(match.getId());
        assertThat(result.roomCode()).isEqualTo("ABC123");
        assertThat(result.status()).isEqualTo(MatchStatus.FINISHED);
        assertThat(result.totalRounds()).isEqualTo(5);
        assertThat(result.finishedAt()).isEqualTo(match.getFinishedAt());
        assertThat(result.ranking()).extracting(entry -> entry.position()).containsExactly(1, 1, 3);
        assertThat(result.ranking()).extracting(entry -> entry.username()).containsExactly("gil", "labubu", "outro");
        assertThat(result.ranking().getFirst().userId()).isEqualTo(first.getUser().getId());
        assertThat(result.ranking().getFirst().avatarKey()).isEqualTo(AvatarKey.AVATAR_01);
        assertThat(result.ranking().getFirst().totalPoints()).isEqualTo(200);
        assertThat(result.ranking().getFirst().correctAnswers()).isEqualTo(3);
        assertThat(first.getTotalPoints()).isEqualTo(200);
        assertThat(first.getPosition()).isEqualTo(1);
        verify(matches, never()).save(any());
        verify(participants, never()).save(any());
        verify(participants, never()).saveAll(any());
    }

    @Test
    void shouldPreserveNullAvatarAndZeroPointTie() {
        participatingPlayer();
        MatchParticipant gil = player("gil", 1, 0, 0);
        MatchParticipant labubu = player("labubu", 1, 0, 0);
        gil.getUser().setAvatarKey(null);
        when(participants.findAllByMatchIdOrderByTotalPointsDescCorrectAnswersDesc(match.getId()))
                .thenReturn(List.of(gil, labubu));
        var result = service.findResult(match.getId(), "gil@email.com");
        assertThat(result.ranking().getFirst().avatarKey()).isNull();
        assertThat(result.ranking()).extracting(entry -> entry.position()).containsExactly(1, 1);
        assertThat(result.ranking()).extracting(entry -> entry.totalPoints()).containsExactly(0, 0);
    }

    @Test
    void shouldRejectNonParticipant() {
        when(matches.findById(match.getId())).thenReturn(Optional.of(match));
        assertThatThrownBy(() -> service.findResult(match.getId(), "outsider@email.com"))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Apenas participantes podem consultar o resultado");
        verify(participants, never()).findAllByMatchIdOrderByTotalPointsDescCorrectAnswersDesc(any());
    }

    @ParameterizedTest
    @EnumSource(value = MatchStatus.class, names = {"IN_PROGRESS", "CANCELLED"})
    void shouldRejectResultBeforeMatchFinishes(MatchStatus status) {
        participatingPlayer();
        match.setStatus(status);
        assertThatThrownBy(() -> service.findResult(match.getId(), "gil@email.com"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("O resultado só está disponível após o término da partida");
        verify(participants, never()).findAllByMatchIdOrderByTotalPointsDescCorrectAnswersDesc(any());
    }

    @Test
    void shouldRejectMissingMatch() {
        when(matches.findById(match.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findResult(match.getId(), "gil@email.com"))
                .isInstanceOf(EntityNotFoundException.class).hasMessage("Partida não encontrada");
        verifyNoInteractions(participants);
    }
}
