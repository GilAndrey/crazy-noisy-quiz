package com.crazynoisyquiz.backend.match.service;

import com.crazynoisyquiz.backend.match.dto.SubmitAnswerRequest;
import com.crazynoisyquiz.backend.match.dto.SubmitAnswerResponse;
import com.crazynoisyquiz.backend.match.model.Match;
import com.crazynoisyquiz.backend.match.model.MatchAnswer;
import com.crazynoisyquiz.backend.match.model.MatchParticipant;
import com.crazynoisyquiz.backend.match.model.MatchRound;
import com.crazynoisyquiz.backend.match.model.MatchStatus;
import com.crazynoisyquiz.backend.match.model.RoundStatus;
import com.crazynoisyquiz.backend.match.repository.MatchAnswerRepository;
import com.crazynoisyquiz.backend.match.repository.MatchParticipantRepository;
import com.crazynoisyquiz.backend.match.repository.MatchRepository;
import com.crazynoisyquiz.backend.match.repository.MatchRoundRepository;
import com.crazynoisyquiz.backend.question.repository.QuestionOptionRepository;
import com.crazynoisyquiz.backend.question.model.QuestionOption;
import com.crazynoisyquiz.backend.shared.exception.ForbiddenOperationException;
import com.crazynoisyquiz.backend.shared.exception.InvalidRequestException;
import com.crazynoisyquiz.backend.shared.exception.ResourceConflictException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MatchAnswerService {
    // Valida a escolha do jogador e registra sua resposta na rodada.

    private final MatchRepository matchRepository;
    private final MatchRoundRepository matchRoundRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final MatchAnswerRepository matchAnswerRepository;

    @Transactional
    public SubmitAnswerResponse submitAnswer(
            UUID matchId,
            UUID roundId,
            String email,
            SubmitAnswerRequest request
    ) {
        // Guarda o horário de chegada antes de esperar pelo bloqueio da partida.
        Instant answeredAt = Instant.now();
        if (request == null || request.optionId() == null) {
            throw new InvalidRequestException("Selecione uma alternativa");
        }

        Match match = matchRepository.findByIdForUpdate(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Partida não encontrada"));

        MatchParticipant participant = matchParticipantRepository
                .findByMatchIdAndUserEmailIgnoreCase(matchId, email)
                .orElseThrow(() -> new ForbiddenOperationException(
                        "Apenas participantes podem responder"
                ));

        if (match.getStatus() != MatchStatus.IN_PROGRESS) {
            throw new ResourceConflictException("A partida não está em andamento");
        }

        MatchRound round = matchRoundRepository.findById(roundId)
                .orElseThrow(() -> new EntityNotFoundException("Rodada não encontrada"));

        if (!round.getMatch().getId().equals(matchId)) {
            throw new InvalidRequestException("A rodada não pertence a essa partida");
        }

        if (round.getStatus() != RoundStatus.IN_PROGRESS
                || !round.getRoundNumber().equals(match.getCurrentRoundNumber())) {
            throw new ResourceConflictException("Essa rodada não está aberta para respostas");
        }

        // O relógio do servidor decide se a resposta chegou dentro do prazo.
        Instant startedAt = round.getStartedAt();
        if (startedAt == null || answeredAt.isBefore(startedAt)) {
            throw new ResourceConflictException("Essa rodada ainda não estava aberta");
        }
        Instant deadline = startedAt.plusSeconds(round.getQuestion().getTimeLimitSeconds());
        if (!answeredAt.isBefore(deadline)) {
            throw new ResourceConflictException("O tempo para responder essa rodada acabou");
        }

        if (matchAnswerRepository.existsByRoundIdAndParticipantId(roundId, participant.getId())) {
            throw new ResourceConflictException("Você já respondeu essa rodada");
        }

        QuestionOption option = questionOptionRepository.findById(request.optionId())
                .orElseThrow(() -> new EntityNotFoundException("Alternativa não encontrada"));
        if (!option.getQuestion().getId().equals(round.getQuestion().getId())) {
            throw new InvalidRequestException("A alternativa não pertence à pergunta dessa rodada");
        }

        MatchAnswer answer = MatchAnswer.builder()
                .round(round)
                .participant(participant)
                .option(option)
                .answeredAt(answeredAt)
                .build();
        MatchAnswer savedAnswer = matchAnswerRepository.save(answer);

        // Confirmamos o registro; o resultado da escolha será mostrado depois.
        return new SubmitAnswerResponse(
                savedAnswer.getId(),
                roundId,
                option.getId(),
                savedAnswer.getAnsweredAt()
        );
    }
}
