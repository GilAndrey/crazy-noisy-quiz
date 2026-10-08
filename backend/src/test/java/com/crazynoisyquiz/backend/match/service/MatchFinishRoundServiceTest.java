package com.crazynoisyquiz.backend.match.service;

import com.crazynoisyquiz.backend.match.model.*;
import com.crazynoisyquiz.backend.match.repository.*;
import com.crazynoisyquiz.backend.question.model.Question;
import com.crazynoisyquiz.backend.question.model.QuestionOption;
import com.crazynoisyquiz.backend.question.repository.QuestionOptionRepository;
import com.crazynoisyquiz.backend.room.model.QuizRoom;
import com.crazynoisyquiz.backend.shared.exception.ForbiddenOperationException;
import com.crazynoisyquiz.backend.shared.exception.ResourceConflictException;
import com.crazynoisyquiz.backend.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchFinishRoundServiceTest {
    @Mock private MatchRepository matches;
    @Mock private MatchRoundRepository rounds;
    @Mock private MatchParticipantRepository participants;
    @Mock private MatchAnswerRepository answers;
    @Mock private QuestionOptionRepository options;
    @Spy private AnswerScoreCalculator calculator = new AnswerScoreCalculator();
    @InjectMocks private MatchService service;
    private Match match;
    private MatchRound round;

    @BeforeEach
    void setUp() {
        User owner = new User();
        owner.setEmail("owner@email.com");
        match = Match.builder().id(UUID.randomUUID()).status(MatchStatus.IN_PROGRESS)
                .currentRoundNumber(1).room(QuizRoom.builder().owner(owner).build()).build();
        Question question = new Question();
        question.setId(UUID.randomUUID());
        question.setTimeLimitSeconds(3600);
        round = MatchRound.builder().id(UUID.randomUUID()).match(match).question(question)
                .roundNumber(1).status(RoundStatus.IN_PROGRESS)
                .startedAt(Instant.now().minusSeconds(1)).build();
    }

    private void existingRound() {
        when(matches.findByIdForUpdate(match.getId())).thenReturn(Optional.of(match));
        when(rounds.findById(round.getId())).thenReturn(Optional.of(round));
    }

    private MatchAnswer answer(boolean correct, int seconds) {
        QuestionOption option = new QuestionOption();
        option.setCorrect(correct);
        MatchParticipant participant = MatchParticipant.builder().totalPoints(20).correctAnswers(1).build();
        return MatchAnswer.builder().round(round).participant(participant).option(option)
                .answeredAt(round.getStartedAt().plusSeconds(seconds)).build();
    }

    private void responseOptions() {
        when(options.findAllByQuestionIdOrderByOptionOrderAsc(round.getQuestion().getId()))
                .thenReturn(List.of());
    }

    @Test
    void shouldFinishEarlyWhenEveryoneAnsweredAndScoreOnlyCorrectChoices() {
        existingRound();
        when(participants.countByMatchId(match.getId())).thenReturn(2L);
        when(answers.countByRoundId(round.getId())).thenReturn(2L);
        MatchAnswer correct = answer(true, 0);
        MatchAnswer wrong = answer(false, 0);
        when(answers.findAllByRoundId(round.getId())).thenReturn(List.of(correct, wrong));
        responseOptions();

        // Encerrar soma ao placar existente, sem zerar as rodadas anteriores.
        var response = service.finishRound(match.getId(), round.getId(), "owner@email.com");
        assertThat(response.status()).isEqualTo(RoundStatus.FINISHED);
        assertThat(round.getEndedAt()).isNotNull();
        assertThat(correct.getParticipant().getTotalPoints()).isEqualTo(120);
        assertThat(correct.getParticipant().getCorrectAnswers()).isEqualTo(2);
        assertThat(wrong.getParticipant().getTotalPoints()).isEqualTo(20);
        assertThat(wrong.getParticipant().getCorrectAnswers()).isEqualTo(1);
        assertThat(match.getCurrentRoundNumber()).isEqualTo(1);

        // Um segundo pedido precisa parar antes de somar o mesmo acerto novamente.
        assertThatThrownBy(() -> service.finishRound(match.getId(), round.getId(), "owner@email.com"))
                .isInstanceOf(ResourceConflictException.class);
        assertThat(correct.getParticipant().getTotalPoints()).isEqualTo(120);
        verify(answers, times(1)).findAllByRoundId(round.getId());
    }

    @Test
    void shouldFinishExpiredRoundWithoutAllAnswers() {
        existingRound();
        round.getQuestion().setTimeLimitSeconds(10);
        round.setStartedAt(Instant.now().minusSeconds(60));
        when(participants.countByMatchId(match.getId())).thenReturn(2L);
        when(answers.countByRoundId(round.getId())).thenReturn(1L);
        MatchAnswer answer = answer(true, 4);
        when(answers.findAllByRoundId(round.getId())).thenReturn(List.of(answer));
        responseOptions();
        service.finishRound(match.getId(), round.getId(), "owner@email.com");
        assertThat(answer.getParticipant().getTotalPoints()).isEqualTo(80);
        assertThat(round.getStatus()).isEqualTo(RoundStatus.FINISHED);
    }

    @Test
    void shouldWaitWhileTimeRemainsAndPlayersHaveNotAnswered() {
        existingRound();
        when(participants.countByMatchId(match.getId())).thenReturn(2L);
        when(answers.countByRoundId(round.getId())).thenReturn(1L);
        assertThatThrownBy(() -> service.finishRound(match.getId(), round.getId(), "owner@email.com"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Aguarde todos responderem ou o tempo da rodada acabar");
        assertThat(round.getStatus()).isEqualTo(RoundStatus.IN_PROGRESS);
        assertThat(round.getEndedAt()).isNull();
        verify(answers, never()).findAllByRoundId(round.getId());
    }

    @Test
    void shouldRejectAnotherPlayer() {
        when(matches.findByIdForUpdate(match.getId())).thenReturn(Optional.of(match));
        assertThatThrownBy(() -> service.finishRound(match.getId(), round.getId(), "player@email.com"))
                .isInstanceOf(ForbiddenOperationException.class);
        verifyNoInteractions(rounds, answers, calculator);
    }
}
