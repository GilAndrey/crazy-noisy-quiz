package com.crazynoisyquiz.backend.match.service;

import com.crazynoisyquiz.backend.match.model.Match;
import com.crazynoisyquiz.backend.match.model.MatchRound;
import com.crazynoisyquiz.backend.match.model.MatchStatus;
import com.crazynoisyquiz.backend.match.model.RoundStatus;
import com.crazynoisyquiz.backend.match.repository.MatchRepository;
import com.crazynoisyquiz.backend.match.repository.MatchParticipantRepository;
import com.crazynoisyquiz.backend.match.repository.MatchRoundRepository;
import com.crazynoisyquiz.backend.question.model.Question;
import com.crazynoisyquiz.backend.question.model.QuestionOption;
import com.crazynoisyquiz.backend.question.repository.QuestionOptionRepository;
import com.crazynoisyquiz.backend.room.model.QuizRoom;
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
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchRoundServiceTest {
    @Mock private MatchRepository matchRepository;
    @Mock private MatchParticipantRepository matchParticipantRepository;
    @Mock private MatchRoundRepository matchRoundRepository;
    @Mock private QuestionOptionRepository questionOptionRepository;
    @InjectMocks private MatchService service;
    private Match match;

    @BeforeEach
    void setUp() {
        User owner = new User();
        owner.setEmail("owner@email.com");
        match = Match.builder().id(UUID.randomUUID())
                .room(QuizRoom.builder().owner(owner).build())
                .status(MatchStatus.IN_PROGRESS).currentRoundNumber(0).totalRounds(5).build();
    }

    // Prepara uma consulta feita por alguém que entrou nessa partida.
    private void participatingPlayer() {
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(matchParticipantRepository.existsByMatchIdAndUserEmailIgnoreCase(
                match.getId(), "player@email.com")).thenReturn(true);
    }

    @Test
    void shouldReadCurrentRoundWithoutRestartingIt() {
        participatingPlayer();
        match.setCurrentRoundNumber(1);
        Instant startedAt = Instant.parse("2026-10-05T17:54:57Z");
        Question question = new Question();
        question.setId(UUID.randomUUID());
        question.setStatement("Qual é o maior oceano da Terra?");
        MatchRound round = MatchRound.builder().id(UUID.randomUUID()).match(match)
                .question(question).roundNumber(1).status(RoundStatus.IN_PROGRESS)
                .startedAt(startedAt).build();
        QuestionOption option = new QuestionOption();
        option.setId(UUID.randomUUID());
        option.setOptionText("Oceano Pacífico");
        option.setOptionOrder(3);
        option.setCorrect(true);
        when(matchRoundRepository.findByMatchIdAndRoundNumber(match.getId(), 1))
                .thenReturn(Optional.of(round));
        when(questionOptionRepository.findAllByQuestionIdOrderByOptionOrderAsc(question.getId()))
                .thenReturn(List.of(option));

        // Consultar a pergunta mantém o horário de abertura e o número da rodada.
        var response = service.findCurrentRound(match.getId(), "player@email.com");
        assertThat(response.id()).isEqualTo(round.getId());
        assertThat(response.matchId()).isEqualTo(match.getId());
        assertThat(response.roundNumber()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(RoundStatus.IN_PROGRESS);
        assertThat(response.startedAt()).isEqualTo(startedAt);
        assertThat(response.statement()).isEqualTo(question.getStatement());
        assertThat(response.timeLimitSeconds()).isEqualTo(10);
        assertThat(response.options().getFirst().id()).isEqualTo(option.getId());
        assertThat(round.getStartedAt()).isEqualTo(startedAt);
        assertThat(round.getStatus()).isEqualTo(RoundStatus.IN_PROGRESS);
        assertThat(match.getCurrentRoundNumber()).isEqualTo(1);
    }

    @Test
    void shouldRejectCurrentRoundForNonParticipant() {
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        // Ter uma conta não basta: o usuário precisa participar dessa partida.
        assertThatThrownBy(() -> service.findCurrentRound(match.getId(), "outsider@email.com"))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Apenas participantes podem consultar a rodada");
        verifyNoInteractions(matchRoundRepository, questionOptionRepository);
    }

    @Test
    void shouldRejectCurrentRoundBeforeAnyRoundIsOpened() {
        participatingPlayer();
        assertThatThrownBy(() -> service.findCurrentRound(match.getId(), "player@email.com"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Nenhuma rodada foi aberta ainda");
        verifyNoInteractions(matchRoundRepository, questionOptionRepository);
    }

    @Test
    void shouldRejectCurrentRoundForFinishedMatch() {
        participatingPlayer();
        match.setStatus(MatchStatus.FINISHED);
        assertThatThrownBy(() -> service.findCurrentRound(match.getId(), "player@email.com"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("A partida não está em andamento");
        verifyNoInteractions(matchRoundRepository, questionOptionRepository);
    }

    @Test
    void shouldRejectCurrentRoundForMissingMatch() {
        when(matchRepository.findById(match.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findCurrentRound(match.getId(), "player@email.com"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Partida não encontrada");
        verifyNoInteractions(matchParticipantRepository, matchRoundRepository, questionOptionRepository);
    }

    @Test
    void shouldRejectMissingCurrentRound() {
        participatingPlayer();
        match.setCurrentRoundNumber(1);
        when(matchRoundRepository.findByMatchIdAndRoundNumber(match.getId(), 1))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findCurrentRound(match.getId(), "player@email.com"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Rodada não encontrada");
        verifyNoInteractions(questionOptionRepository);
    }

    @Test
    void shouldRejectCurrentRoundThatHasFinished() {
        participatingPlayer();
        match.setCurrentRoundNumber(1);
        MatchRound round = MatchRound.builder().status(RoundStatus.FINISHED).build();
        when(matchRoundRepository.findByMatchIdAndRoundNumber(match.getId(), 1))
                .thenReturn(Optional.of(round));
        assertThatThrownBy(() -> service.findCurrentRound(match.getId(), "player@email.com"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("A rodada atual não está em andamento");
        verifyNoInteractions(questionOptionRepository);
    }

    private void existingMatch() {
        when(matchRepository.findByIdForUpdate(match.getId())).thenReturn(Optional.of(match));
    }

    @Test
    void shouldOpenFirstRoundAndReturnQuestion() {
        existingMatch();
        Question question = new Question();
        question.setId(UUID.randomUUID());
        question.setStatement("Quanto é 2 + 2?");
        MatchRound round = MatchRound.builder().id(UUID.randomUUID()).match(match)
                .question(question).roundNumber(1).status(RoundStatus.PENDING).build();
        QuestionOption option = new QuestionOption();
        option.setId(UUID.randomUUID());
        option.setOptionText("4");
        option.setOptionOrder(1);
        option.setCorrect(true);
        when(matchRoundRepository.findByMatchIdAndRoundNumber(match.getId(), 1))
                .thenReturn(Optional.of(round));
        when(questionOptionRepository.findAllByQuestionIdOrderByOptionOrderAsc(question.getId()))
                .thenReturn(List.of(option));

        // Abrir a rodada atualiza as entidades e devolve os dados que o jogador precisa.
        var response = service.startNextRound(match.getId(), "owner@email.com");
        assertThat(match.getCurrentRoundNumber()).isEqualTo(1);
        assertThat(round.getStatus()).isEqualTo(RoundStatus.IN_PROGRESS);
        assertThat(round.getStartedAt()).isNotNull();
        assertThat(response.startedAt()).isEqualTo(round.getStartedAt());
        assertThat(response.questionId()).isEqualTo(question.getId());
        assertThat(response.options().getFirst().optionText()).isEqualTo("4");
    }

    @Test
    void shouldRejectMissingMatch() {
        when(matchRepository.findByIdForUpdate(match.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.startNextRound(match.getId(), "owner@email.com"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldRejectAnotherPlayer() {
        existingMatch();
        assertThatThrownBy(() -> service.startNextRound(match.getId(), "player@email.com"))
                .isInstanceOf(ForbiddenOperationException.class);
        verifyNoInteractions(matchRoundRepository, questionOptionRepository);
    }

    @Test
    void shouldRejectFinishedMatch() {
        existingMatch();
        match.setStatus(MatchStatus.FINISHED);
        assertThatThrownBy(() -> service.startNextRound(match.getId(), "owner@email.com"))
                .isInstanceOf(ResourceConflictException.class);
        verifyNoInteractions(matchRoundRepository, questionOptionRepository);
    }

    @Test
    void shouldRejectAnOpenRoundWithoutAdvancingMatch() {
        existingMatch();
        when(matchRoundRepository.existsByMatchIdAndStatus(match.getId(), RoundStatus.IN_PROGRESS))
                .thenReturn(true);
        // Um segundo clique não pode avançar para outra pergunta.
        assertThatThrownBy(() -> service.startNextRound(match.getId(), "owner@email.com"))
                .isInstanceOf(ResourceConflictException.class);
        assertThat(match.getCurrentRoundNumber()).isZero();
        verifyNoInteractions(questionOptionRepository);
    }

    @Test
    void shouldRejectWhenAllRoundsWereOpened() {
        existingMatch();
        match.setCurrentRoundNumber(5);
        assertThatThrownBy(() -> service.startNextRound(match.getId(), "owner@email.com"))
                .isInstanceOf(ResourceConflictException.class);
        verifyNoInteractions(questionOptionRepository);
    }

    @Test
    void shouldRejectMissingRound() {
        existingMatch();
        when(matchRoundRepository.findByMatchIdAndRoundNumber(match.getId(), 1))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.startNextRound(match.getId(), "owner@email.com"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldRejectRoundThatIsNotPending() {
        existingMatch();
        MatchRound round = MatchRound.builder().status(RoundStatus.FINISHED).build();
        when(matchRoundRepository.findByMatchIdAndRoundNumber(match.getId(), 1))
                .thenReturn(Optional.of(round));
        assertThatThrownBy(() -> service.startNextRound(match.getId(), "owner@email.com"))
                .isInstanceOf(ResourceConflictException.class);
        assertThat(round.getStartedAt()).isNull();
        assertThat(match.getCurrentRoundNumber()).isZero();
    }
}
