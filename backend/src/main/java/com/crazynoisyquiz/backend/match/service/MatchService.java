package com.crazynoisyquiz.backend.match.service;

import com.crazynoisyquiz.backend.match.dto.MatchResponse;
import com.crazynoisyquiz.backend.match.dto.MatchRoundResponse;
import com.crazynoisyquiz.backend.match.dto.QuestionOptionResponse;
import com.crazynoisyquiz.backend.match.dto.StartMatchRequest;
import com.crazynoisyquiz.backend.match.model.Match;
import com.crazynoisyquiz.backend.match.model.MatchAnswer;
import com.crazynoisyquiz.backend.match.model.MatchParticipant;
import com.crazynoisyquiz.backend.match.model.MatchRound;
import com.crazynoisyquiz.backend.match.model.MatchStatus;
import com.crazynoisyquiz.backend.match.model.RoundStatus;
import com.crazynoisyquiz.backend.match.repository.MatchParticipantRepository;
import com.crazynoisyquiz.backend.match.repository.MatchAnswerRepository;
import com.crazynoisyquiz.backend.match.repository.MatchRepository;
import com.crazynoisyquiz.backend.match.repository.MatchRoundRepository;
import com.crazynoisyquiz.backend.question.model.Category;
import com.crazynoisyquiz.backend.question.model.Question;
import com.crazynoisyquiz.backend.question.repository.CategoryRepository;
import com.crazynoisyquiz.backend.question.repository.QuestionRepository;
import com.crazynoisyquiz.backend.question.repository.QuestionOptionRepository;
import com.crazynoisyquiz.backend.room.model.QuizRoom;
import com.crazynoisyquiz.backend.room.model.RoomParticipant;
import com.crazynoisyquiz.backend.room.model.RoomStatus;
import com.crazynoisyquiz.backend.room.repository.QuizRoomRepository;
import com.crazynoisyquiz.backend.room.repository.RoomParticipantRepository;
import com.crazynoisyquiz.backend.shared.exception.ForbiddenOperationException;
import com.crazynoisyquiz.backend.shared.exception.InvalidRequestException;
import com.crazynoisyquiz.backend.shared.exception.ResourceConflictException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
// Valida as regras e cria a partida, os participantes e as rodadas em uma única transação.
public class MatchService {

    private static final int MIN_ROUNDS = 5;
    private static final int MAX_ROUNDS = 20;
    private static final int MIN_PARTICIPANTS = 2;

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final MatchRoundRepository matchRoundRepository;
    private final QuizRoomRepository quizRoomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final CategoryRepository categoryRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final MatchAnswerRepository matchAnswerRepository;
    private final AnswerScoreCalculator answerScoreCalculator;

    @Transactional
    public MatchResponse startMatch(
            String roomCode,
            String userEmail,
            StartMatchRequest request
    ) {
        if (request == null || request.categoryIds() == null || request.categoryIds().isEmpty()) {
            throw new InvalidRequestException("Selecione pelo menos uma categoria");
        }

        QuizRoom room = quizRoomRepository.findByCodeForUpdate(roomCode)
                .orElseThrow(() -> new EntityNotFoundException("Sala não encontrada"));

        if (!room.getOwner().getEmail().equalsIgnoreCase(userEmail)) {
            throw new ForbiddenOperationException("Apenas o criador da sala pode iniciar a partida");
        }

        if (room.getStatus() != RoomStatus.WAITING) {
            throw new ResourceConflictException("A sala não está aguardando uma partida");
        }

        if (request.totalRounds() == null
                || request.totalRounds() < MIN_ROUNDS
                || request.totalRounds() > MAX_ROUNDS) {
            throw new InvalidRequestException("A partida deve ter entre 5 e 20 rodadas");
        }

        List<UUID> requestedCategoryIds = request.categoryIds();
        Set<UUID> uniqueCategoryIds = new HashSet<>(requestedCategoryIds);
        if (uniqueCategoryIds.size() != requestedCategoryIds.size()) {
            throw new InvalidRequestException("Não é permitido repetir categorias");
        }

        long activeParticipants = roomParticipantRepository
                .countByRoomIdAndLeftAtIsNull(room.getId());
        if (activeParticipants < MIN_PARTICIPANTS) {
            throw new ResourceConflictException("São necessários pelo menos 2 participantes ativos");
        }

        List<Category> categories = categoryRepository
                .findAllByIdInAndActiveTrue(requestedCategoryIds);
        if (categories.size() != requestedCategoryIds.size()) {
            throw new InvalidRequestException("Uma ou mais categorias não existem ou estão inativas");
        }

        List<Question> availableQuestions = questionRepository
                .findAllByCategoryIdInAndActiveTrue(requestedCategoryIds);
        if (availableQuestions.size() < request.totalRounds()) {
            throw new InvalidRequestException(
                    "As categorias escolhidas não têm perguntas suficientes para essa partida"
            );
        }

        List<Question> selectedQuestions = selectQuestionsWithoutRepeating(
                requestedCategoryIds,
                availableQuestions,
                request.totalRounds()
        );

        Match match = Match.builder()
                .room(room)
                .categories(new HashSet<>(categories))
                .status(MatchStatus.IN_PROGRESS)
                .totalRounds(request.totalRounds())
                .currentRoundNumber(0)
                .startedAt(Instant.now())
                .createdAt(Instant.now())
                .build();
        Match savedMatch = matchRepository.save(match);

        List<RoomParticipant> activeRoomParticipants = roomParticipantRepository
                .findAllByRoomIdAndLeftAtIsNull(room.getId());
        List<MatchParticipant> matchParticipants = activeRoomParticipants.stream()
                .map(roomParticipant -> MatchParticipant.builder()
                        .match(savedMatch)
                        .user(roomParticipant.getUser())
                        .totalPoints(0)
                        .correctAnswers(0)
                        .joinedAt(Instant.now())
                        .build())
                .toList();
        matchParticipantRepository.saveAll(matchParticipants);

        List<MatchRound> rounds = new ArrayList<>();
        for (int index = 0; index < selectedQuestions.size(); index++) {
            rounds.add(MatchRound.builder()
                    .match(savedMatch)
                    .question(selectedQuestions.get(index))
                    .roundNumber(index + 1)
                    .status(RoundStatus.PENDING)
                    .build());
        }
        matchRoundRepository.saveAll(rounds);

        room.setStatus(RoomStatus.IN_PROGRESS);

        return new MatchResponse(
                savedMatch.getId(),
                room.getCode(),
                savedMatch.getStatus(),
                savedMatch.getTotalRounds(),
                savedMatch.getCurrentRoundNumber(),
                requestedCategoryIds
        );
    }

    // Faz rodízio entre as categorias para manter a seleção equilibrada quando houver estoque.
    private List<Question> selectQuestionsWithoutRepeating(
            List<UUID> categoryIds,
            List<Question> availableQuestions,
            int totalRounds
    ) {
        Map<UUID, List<Question>> questionsByCategory = new HashMap<>();
        for (UUID categoryId : categoryIds) {
            questionsByCategory.put(categoryId, new ArrayList<>());
        }
        for (Question question : availableQuestions) {
            questionsByCategory.get(question.getCategory().getId()).add(question);
        }
        questionsByCategory.values().forEach(Collections::shuffle);

        List<Question> selected = new ArrayList<>(totalRounds);
        int questionIndex = 0;
        while (selected.size() < totalRounds) {
            boolean addedQuestionThisPass = false;
            for (UUID categoryId : categoryIds) {
                List<Question> categoryQuestions = questionsByCategory.get(categoryId);
                if (questionIndex < categoryQuestions.size()) {
                    selected.add(categoryQuestions.get(questionIndex));
                    addedQuestionThisPass = true;
                    if (selected.size() == totalRounds) {
                        break;
                    }
                }
            }
            if (!addedQuestionThisPass) {
                throw new InvalidRequestException(
                        "As categorias escolhidas não têm perguntas suficientes para essa partida"
                );
            }
            questionIndex++;
        }
        return selected;
    }

    // Abre a próxima rodada e entrega a pergunta aos jogadores.
    @Transactional
    public MatchRoundResponse startNextRound(UUID matchId, String email) {
        Match match = matchRepository.findByIdForUpdate(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Partida não encontrada"));

        if (!match.getRoom().getOwner().getEmail().equalsIgnoreCase(email)) {
            throw new ForbiddenOperationException(
                    "Apenas o criador da sala pode abrir a próxima rodada"
            );
        }

        if (match.getStatus() != MatchStatus.IN_PROGRESS) {
            throw new ResourceConflictException("A partida não está em andamento");
        }

        if (matchRoundRepository.existsByMatchIdAndStatus(matchId, RoundStatus.IN_PROGRESS)) {
            throw new ResourceConflictException(
                    "A rodada atual precisa terminar antes de abrir a próxima"
            );
        }

        int nextRoundNumber = match.getCurrentRoundNumber() + 1;
        if (nextRoundNumber > match.getTotalRounds()) {
            throw new ResourceConflictException("Todas as rodadas da partida já foram abertas");
        }

        MatchRound round = matchRoundRepository
                .findByMatchIdAndRoundNumber(matchId, nextRoundNumber)
                .orElseThrow(() -> new EntityNotFoundException("Rodada não encontrada"));

        if (round.getStatus() != RoundStatus.PENDING) {
            throw new ResourceConflictException("Essa rodada já foi aberta");
        }

        round.setStatus(RoundStatus.IN_PROGRESS);
        round.setStartedAt(Instant.now());
        match.setCurrentRoundNumber(nextRoundNumber);
        // A transação salva as alterações nas entidades que acabamos de buscar.

        return toRoundResponse(round);
    }

    // Busca a pergunta atual apenas para quem participa dessa partida.
    @Transactional(readOnly = true)
    public MatchRoundResponse findCurrentRound(UUID matchId, String email) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Partida não encontrada"));

        if (!matchParticipantRepository.existsByMatchIdAndUserEmailIgnoreCase(matchId, email)) {
            throw new ForbiddenOperationException("Apenas participantes podem consultar a rodada");
        }

        if (match.getStatus() != MatchStatus.IN_PROGRESS) {
            throw new ResourceConflictException("A partida não está em andamento");
        }

        if (match.getCurrentRoundNumber() == 0) {
            throw new ResourceConflictException("Nenhuma rodada foi aberta ainda");
        }

        MatchRound round = matchRoundRepository
                .findByMatchIdAndRoundNumber(matchId, match.getCurrentRoundNumber())
                .orElseThrow(() -> new EntityNotFoundException("Rodada não encontrada"));

        if (round.getStatus() != RoundStatus.IN_PROGRESS) {
            throw new ResourceConflictException("A rodada atual não está em andamento");
        }

        return toRoundResponse(round);
    }

    // Encerra a rodada e soma os pontos uma única vez.
    @Transactional
    public MatchRoundResponse finishRound(UUID matchId, UUID roundId, String email) {
        Match match = matchRepository.findByIdForUpdate(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Partida não encontrada"));

        if (!match.getRoom().getOwner().getEmail().equalsIgnoreCase(email)) {
            throw new ForbiddenOperationException("Apenas o criador da sala pode encerrar a rodada");
        }
        if (match.getStatus() != MatchStatus.IN_PROGRESS) {
            throw new ResourceConflictException("A partida não está em andamento");
        }

        MatchRound round = matchRoundRepository.findById(roundId)
                .orElseThrow(() -> new EntityNotFoundException("Rodada não encontrada"));
        if (!round.getMatch().getId().equals(matchId)) {
            throw new InvalidRequestException("A rodada não pertence a essa partida");
        }
        // Esse teste acontece antes da pontuação, inclusive em um segundo pedido.
        if (round.getStatus() != RoundStatus.IN_PROGRESS
                || !round.getRoundNumber().equals(match.getCurrentRoundNumber())) {
            throw new ResourceConflictException("Essa rodada não está em andamento");
        }
        if (round.getStartedAt() == null) {
            throw new ResourceConflictException("Essa rodada ainda não foi aberta");
        }

        Instant endedAt = Instant.now();
        int timeLimit = round.getQuestion().getTimeLimitSeconds();
        boolean timeExpired = !endedAt.isBefore(round.getStartedAt().plusSeconds(timeLimit));
        long participantCount = matchParticipantRepository.countByMatchId(matchId);
        long answerCount = matchAnswerRepository.countByRoundId(roundId);
        if (!timeExpired && answerCount < participantCount) {
            throw new ResourceConflictException("Aguarde todos responderem ou o tempo da rodada acabar");
        }

        List<MatchAnswer> answers = matchAnswerRepository.findAllByRoundId(roundId);
        for (MatchAnswer answer : answers) {
            int points = answerScoreCalculator.calculate(
                    answer.getOption().isCorrect(),
                    round.getStartedAt(),
                    answer.getAnsweredAt(),
                    timeLimit
            );
            MatchParticipant participant = answer.getParticipant();
            participant.setTotalPoints(participant.getTotalPoints() + points);
            // Toda resposta correta dentro do prazo recebe pontos nas faixas atuais.
            if (points > 0) {
                participant.setCorrectAnswers(participant.getCorrectAnswers() + 1);
            }
        }

        round.setStatus(RoundStatus.FINISHED);
        round.setEndedAt(endedAt);
        // A transação salva a rodada e os totais dos participantes juntos.
        return toRoundResponse(round);
    }

    // Mantém a mesma resposta ao abrir, consultar e encerrar uma rodada.
    private MatchRoundResponse toRoundResponse(MatchRound round) {
        Question question = round.getQuestion();
        // O jogador recebe as alternativas em ordem, sem o campo que revela o gabarito.
        List<QuestionOptionResponse> options = questionOptionRepository
                .findAllByQuestionIdOrderByOptionOrderAsc(question.getId())
                .stream()
                .map(option -> new QuestionOptionResponse(
                        option.getId(), option.getOptionText(), option.getOptionOrder()
                ))
                .toList();

        return new MatchRoundResponse(
                round.getId(),
                round.getMatch().getId(),
                round.getRoundNumber(),
                round.getStatus(),
                round.getStartedAt(),
                question.getId(),
                question.getStatement(),
                question.getTimeLimitSeconds(),
                options
        );
    }

}
