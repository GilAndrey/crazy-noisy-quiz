package com.crazynoisyquiz.backend.match.service;

import com.crazynoisyquiz.backend.match.dto.MatchResponse;
import com.crazynoisyquiz.backend.match.dto.StartMatchRequest;
import com.crazynoisyquiz.backend.match.model.Match;
import com.crazynoisyquiz.backend.match.model.MatchParticipant;
import com.crazynoisyquiz.backend.match.model.MatchRound;
import com.crazynoisyquiz.backend.match.model.MatchStatus;
import com.crazynoisyquiz.backend.match.model.RoundStatus;
import com.crazynoisyquiz.backend.match.repository.MatchParticipantRepository;
import com.crazynoisyquiz.backend.match.repository.MatchRepository;
import com.crazynoisyquiz.backend.match.repository.MatchRoundRepository;
import com.crazynoisyquiz.backend.question.model.Category;
import com.crazynoisyquiz.backend.question.model.Question;
import com.crazynoisyquiz.backend.question.repository.CategoryRepository;
import com.crazynoisyquiz.backend.question.repository.QuestionRepository;
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
}
