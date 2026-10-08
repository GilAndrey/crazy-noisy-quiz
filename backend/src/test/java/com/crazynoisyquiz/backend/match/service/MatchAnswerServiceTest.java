package com.crazynoisyquiz.backend.match.service;

import com.crazynoisyquiz.backend.match.dto.SubmitAnswerRequest;
import com.crazynoisyquiz.backend.match.model.*;
import com.crazynoisyquiz.backend.match.repository.*;
import com.crazynoisyquiz.backend.question.model.Question;
import com.crazynoisyquiz.backend.question.model.QuestionOption;
import com.crazynoisyquiz.backend.question.repository.QuestionOptionRepository;
import com.crazynoisyquiz.backend.shared.exception.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchAnswerServiceTest {
    @Mock private MatchRepository matches;
    @Mock private MatchRoundRepository rounds;
    @Mock private MatchParticipantRepository participants;
    @Mock private QuestionOptionRepository options;
    @Mock private MatchAnswerRepository answers;
    @InjectMocks private MatchAnswerService service;

    private Match match;
    private MatchRound round;
    private MatchParticipant participant;
    private QuestionOption option;
    private SubmitAnswerRequest request;

    @BeforeEach
    void setUp() {
        match = Match.builder().id(UUID.randomUUID()).status(MatchStatus.IN_PROGRESS)
                .currentRoundNumber(1).build();
        Question question = new Question();
        question.setId(UUID.randomUUID());
        // Uma margem ampla deixa os testes comuns independentes da velocidade da máquina.
        question.setTimeLimitSeconds(3600);
        round = MatchRound.builder().id(UUID.randomUUID()).match(match).question(question)
                .roundNumber(1).status(RoundStatus.IN_PROGRESS)
                .startedAt(Instant.now().minusSeconds(1)).build();
        participant = MatchParticipant.builder().id(UUID.randomUUID()).match(match)
                .totalPoints(0).correctAnswers(0).build();
        option = new QuestionOption();
        option.setId(UUID.randomUUID());
        option.setQuestion(question);
        request = new SubmitAnswerRequest(option.getId());
    }

    private void existingMatch() {
        when(matches.findByIdForUpdate(match.getId())).thenReturn(Optional.of(match));
    }

    private void participatingPlayer() {
        existingMatch();
        when(participants.findByMatchIdAndUserEmailIgnoreCase(match.getId(), "player@email.com"))
                .thenReturn(Optional.of(participant));
    }

    private void existingRound() {
        participatingPlayer();
        when(rounds.findById(round.getId())).thenReturn(Optional.of(round));
    }

    private void reject(Class<? extends Throwable> type, String message) {
        assertThatThrownBy(() -> service.submitAnswer(
                match.getId(), round.getId(), "player@email.com", request))
                .isInstanceOf(type).hasMessage(message);
        verify(answers, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldSaveCorrectOrWrongChoiceWithoutChangingScore(boolean correct) {
        existingRound();
        option.setCorrect(correct);
        when(options.findById(option.getId())).thenReturn(Optional.of(option));
        UUID answerId = UUID.randomUUID();
        when(answers.save(any(MatchAnswer.class))).thenAnswer(invocation -> {
            MatchAnswer answer = invocation.getArgument(0);
            answer.setId(answerId);
            return answer;
        });
        Instant before = Instant.now();

        // Acerto e erro são registrados igualmente; pontuação vem em outro passo.
        var response = service.submitAnswer(match.getId(), round.getId(), "player@email.com", request);
        var captor = org.mockito.ArgumentCaptor.forClass(MatchAnswer.class);
        verify(answers).save(captor.capture());
        assertThat(captor.getValue().getRound()).isSameAs(round);
        assertThat(captor.getValue().getParticipant()).isSameAs(participant);
        assertThat(captor.getValue().getOption()).isSameAs(option);
        assertThat(response.id()).isEqualTo(answerId);
        assertThat(response.roundId()).isEqualTo(round.getId());
        assertThat(response.optionId()).isEqualTo(option.getId());
        assertThat(response.answeredAt()).isBetween(before, Instant.now());
        assertThat(response.answeredAt()).isEqualTo(captor.getValue().getAnsweredAt());
        assertThat(match.getCurrentRoundNumber()).isEqualTo(1);
        assertThat(participant.getTotalPoints()).isZero();
        assertThat(participant.getCorrectAnswers()).isZero();
    }

    @Test
    void shouldRejectMissingOption() {
        request = new SubmitAnswerRequest(null);
        reject(InvalidRequestException.class, "Selecione uma alternativa");
        verifyNoInteractions(matches);
    }

    @Test
    void shouldRejectMissingMatch() {
        when(matches.findByIdForUpdate(match.getId())).thenReturn(Optional.empty());
        reject(EntityNotFoundException.class, "Partida não encontrada");
    }

    @Test
    void shouldRejectNonParticipant() {
        existingMatch();
        reject(ForbiddenOperationException.class, "Apenas participantes podem responder");
        verifyNoInteractions(rounds, options);
    }

    @Test
    void shouldRejectFinishedMatch() {
        participatingPlayer();
        match.setStatus(MatchStatus.FINISHED);
        reject(ResourceConflictException.class, "A partida não está em andamento");
    }

    @Test
    void shouldRejectMissingRound() {
        participatingPlayer();
        when(rounds.findById(round.getId())).thenReturn(Optional.empty());
        reject(EntityNotFoundException.class, "Rodada não encontrada");
    }

    @Test
    void shouldRejectRoundFromAnotherMatch() {
        existingRound();
        round.setMatch(Match.builder().id(UUID.randomUUID()).build());
        reject(InvalidRequestException.class, "A rodada não pertence a essa partida");
    }

    @Test
    void shouldRejectFinishedRound() {
        existingRound();
        round.setStatus(RoundStatus.FINISHED);
        reject(ResourceConflictException.class, "Essa rodada não está aberta para respostas");
    }

    @Test
    void shouldRejectNonCurrentRound() {
        existingRound();
        round.setRoundNumber(2);
        reject(ResourceConflictException.class, "Essa rodada não está aberta para respostas");
    }

    @Test
    void shouldRejectExpiredAnswer() {
        existingRound();
        round.setStartedAt(Instant.now().minusSeconds(7200));
        // Uma rodada ainda marcada como aberta também precisa respeitar o prazo.
        reject(ResourceConflictException.class, "O tempo para responder essa rodada acabou");
        verifyNoInteractions(options);
    }

    @Test
    void shouldRejectAnswerExactlyAtDeadline() {
        existingRound();
        Instant deadline = Instant.parse("2026-10-07T12:00:10Z");
        round.setStartedAt(deadline.minusSeconds(10));
        round.getQuestion().setTimeLimitSeconds(10);
        // Congelamos o relógio para testar a fronteira exata sem usar sleep.
        try (var clock = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
            clock.when(Instant::now).thenReturn(deadline);
            reject(ResourceConflictException.class, "O tempo para responder essa rodada acabou");
        }
    }

    @Test
    void shouldRejectRoundWithoutStartTime() {
        existingRound();
        round.setStartedAt(null);
        reject(ResourceConflictException.class, "Essa rodada ainda não estava aberta");
    }

    @Test
    void shouldRejectAnswerBeforeRoundStarted() {
        existingRound();
        round.setStartedAt(Instant.now().plusSeconds(3600));
        reject(ResourceConflictException.class, "Essa rodada ainda não estava aberta");
    }

    @Test
    void shouldRejectDuplicateAnswer() {
        existingRound();
        when(answers.existsByRoundIdAndParticipantId(round.getId(), participant.getId()))
                .thenReturn(true);
        // O jogador não pode trocar sua escolha enviando outra requisição.
        reject(ResourceConflictException.class, "Você já respondeu essa rodada");
        verifyNoInteractions(options);
    }

    @Test
    void shouldRejectMissingAlternative() {
        existingRound();
        when(options.findById(option.getId())).thenReturn(Optional.empty());
        reject(EntityNotFoundException.class, "Alternativa não encontrada");
    }

    @Test
    void shouldRejectAlternativeFromAnotherQuestion() {
        existingRound();
        Question another = new Question();
        another.setId(UUID.randomUUID());
        option.setQuestion(another);
        when(options.findById(option.getId())).thenReturn(Optional.of(option));
        reject(InvalidRequestException.class, "A alternativa não pertence à pergunta dessa rodada");
    }
}
