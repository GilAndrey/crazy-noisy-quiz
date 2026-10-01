package com.crazynoisyquiz.backend.match.service;

import com.crazynoisyquiz.backend.match.dto.MatchResponse;
import com.crazynoisyquiz.backend.match.dto.StartMatchRequest;
import com.crazynoisyquiz.backend.match.model.Match;
import com.crazynoisyquiz.backend.match.model.MatchParticipant;
import com.crazynoisyquiz.backend.match.model.MatchRound;
import com.crazynoisyquiz.backend.match.model.MatchStatus;
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
import com.crazynoisyquiz.backend.user.model.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock private MatchRepository matchRepository;
    @Mock private MatchParticipantRepository matchParticipantRepository;
    @Mock private MatchRoundRepository matchRoundRepository;
    @Mock private QuizRoomRepository quizRoomRepository;
    @Mock private RoomParticipantRepository roomParticipantRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private QuestionRepository questionRepository;

    @InjectMocks
    private MatchService matchService;

    private UUID roomId;
    private UUID categoryId;
    private QuizRoom room;
    private List<RoomParticipant> activeParticipants;
    private List<Category> categories;
    private List<Question> questions;

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("owner@email.com");

        room = QuizRoom.builder()
                .id(roomId)
                .code("ABC123")
                .owner(owner)
                .status(RoomStatus.WAITING)
                .maxPlayers(8)
                .build();

        User secondPlayer = new User();
        secondPlayer.setId(UUID.randomUUID());
        secondPlayer.setEmail("player@email.com");

        RoomParticipant ownerParticipation = new RoomParticipant();
        ownerParticipation.setRoom(room);
        ownerParticipation.setUser(owner);
        RoomParticipant secondParticipation = new RoomParticipant();
        secondParticipation.setRoom(room);
        secondParticipation.setUser(secondPlayer);
        activeParticipants = List.of(ownerParticipation, secondParticipation);

        Category category = new Category();
        category.setId(categoryId);
        category.setName("Ciência");
        category.setActive(true);
        categories = List.of(category);

        questions = List.of(
                question(category), question(category), question(category),
                question(category), question(category)
        );
    }

    @Test
    void shouldCreateMatchWithParticipantsAndUniqueQuestions() {
        UUID matchId = UUID.randomUUID();
        when(quizRoomRepository.findByCodeForUpdate("ABC123")).thenReturn(Optional.of(room));
        when(roomParticipantRepository.countByRoomIdAndLeftAtIsNull(roomId)).thenReturn(2L);
        when(roomParticipantRepository.findAllByRoomIdAndLeftAtIsNull(roomId))
                .thenReturn(activeParticipants);
        when(categoryRepository.findAllByIdInAndActiveTrue(List.of(categoryId)))
                .thenReturn(categories);
        when(questionRepository.findAllByCategoryIdInAndActiveTrue(List.of(categoryId)))
                .thenReturn(questions);
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
            Match match = invocation.getArgument(0);
            match.setId(matchId);
            return match;
        });
        when(matchParticipantRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchRoundRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // A criação registra a partida, todos os jogadores ativos e uma pergunta por rodada.
        MatchResponse response = matchService.startMatch(
                "ABC123",
                "owner@email.com",
                new StartMatchRequest(List.of(categoryId), 5)
        );

        assertThat(response.id()).isEqualTo(matchId);
        assertThat(response.status()).isEqualTo(MatchStatus.IN_PROGRESS);
        assertThat(response.totalRounds()).isEqualTo(5);
        assertThat(response.currentRoundNumber()).isZero();
        assertThat(response.categoryIds()).containsExactly(categoryId);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.IN_PROGRESS);

        ArgumentCaptor<List<MatchParticipant>> participantsCaptor = ArgumentCaptor.forClass(List.class);
        verify(matchParticipantRepository).saveAll(participantsCaptor.capture());
        assertThat(participantsCaptor.getValue()).hasSize(2);

        ArgumentCaptor<List<MatchRound>> roundsCaptor = ArgumentCaptor.forClass(List.class);
        verify(matchRoundRepository).saveAll(roundsCaptor.capture());
        assertThat(roundsCaptor.getValue()).hasSize(5);
        assertThat(roundsCaptor.getValue()).extracting(MatchRound::getRoundNumber)
                .containsExactly(1, 2, 3, 4, 5);
        assertThat(roundsCaptor.getValue()).extracting(round -> round.getQuestion().getId())
                .doesNotHaveDuplicates();
    }

    @Test
    void shouldRejectStartingMatchWhenCallerIsNotRoomOwner() {
        when(quizRoomRepository.findByCodeForUpdate("ABC123")).thenReturn(Optional.of(room));

        // Um jogador comum não pode iniciar partida na sala de outra pessoa.
        assertThatThrownBy(() -> matchService.startMatch(
                "ABC123", "player@email.com", new StartMatchRequest(List.of(categoryId), 5)
        )).isInstanceOf(ForbiddenOperationException.class);

        verify(matchRepository, never()).save(any());
    }

    @Test
    void shouldRejectStartingMatchWhenRoomHasLessThanTwoActivePlayers() {
        when(quizRoomRepository.findByCodeForUpdate("ABC123")).thenReturn(Optional.of(room));
        when(roomParticipantRepository.countByRoomIdAndLeftAtIsNull(roomId)).thenReturn(1L);

        // A partida precisa de pelo menos duas pessoas ainda presentes na sala.
        assertThatThrownBy(() -> matchService.startMatch(
                "ABC123", "owner@email.com", new StartMatchRequest(List.of(categoryId), 5)
        )).isInstanceOf(ResourceConflictException.class);

        verify(categoryRepository, never()).findAllByIdInAndActiveTrue(any());
    }

    @Test
    void shouldRejectStartingMatchWhenThereAreNotEnoughQuestions() {
        when(quizRoomRepository.findByCodeForUpdate("ABC123")).thenReturn(Optional.of(room));
        when(roomParticipantRepository.countByRoomIdAndLeftAtIsNull(roomId)).thenReturn(2L);
        when(categoryRepository.findAllByIdInAndActiveTrue(List.of(categoryId)))
                .thenReturn(categories);
        when(questionRepository.findAllByCategoryIdInAndActiveTrue(List.of(categoryId)))
                .thenReturn(questions.subList(0, 4));

        // Sem cinco perguntas distintas, a partida não deve ser gravada.
        assertThatThrownBy(() -> matchService.startMatch(
                "ABC123", "owner@email.com", new StartMatchRequest(List.of(categoryId), 5)
        )).isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("perguntas suficientes");

        verify(matchRepository, never()).save(any());
    }

    @Test
    void shouldDistributeSelectedQuestionsAcrossCategoriesWhenPossible() {
        UUID secondCategoryId = UUID.randomUUID();
        Category secondCategory = new Category();
        secondCategory.setId(secondCategoryId);
        secondCategory.setName("História");
        secondCategory.setActive(true);

        List<UUID> selectedCategoryIds = List.of(categoryId, secondCategoryId);
        List<Question> pooledQuestions = List.of(
                question(categories.get(0)), question(categories.get(0)),
                question(categories.get(0)), question(categories.get(0)),
                question(secondCategory), question(secondCategory),
                question(secondCategory), question(secondCategory)
        );

        when(quizRoomRepository.findByCodeForUpdate("ABC123")).thenReturn(Optional.of(room));
        when(roomParticipantRepository.countByRoomIdAndLeftAtIsNull(roomId)).thenReturn(2L);
        when(roomParticipantRepository.findAllByRoomIdAndLeftAtIsNull(roomId))
                .thenReturn(activeParticipants);
        when(categoryRepository.findAllByIdInAndActiveTrue(selectedCategoryIds))
                .thenReturn(List.of(categories.get(0), secondCategory));
        when(questionRepository.findAllByCategoryIdInAndActiveTrue(selectedCategoryIds))
                .thenReturn(pooledQuestions);
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
            Match match = invocation.getArgument(0);
            match.setId(UUID.randomUUID());
            return match;
        });

        // Quando ambas têm estoque, cada categoria deve contribuir igualmente para seis rodadas.
        matchService.startMatch(
                "ABC123", "owner@email.com", new StartMatchRequest(selectedCategoryIds, 6)
        );

        ArgumentCaptor<List<MatchRound>> roundsCaptor = ArgumentCaptor.forClass(List.class);
        verify(matchRoundRepository).saveAll(roundsCaptor.capture());
        assertThat(roundsCaptor.getValue())
                .extracting(round -> round.getQuestion().getCategory().getId())
                .containsExactlyInAnyOrder(
                        categoryId, categoryId, categoryId,
                        secondCategoryId, secondCategoryId, secondCategoryId
                );
    }

    @Test
    void shouldReturnNotFoundWhenRoomDoesNotExist() {
        when(quizRoomRepository.findByCodeForUpdate("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.startMatch(
                "MISSING", "owner@email.com", new StartMatchRequest(List.of(categoryId), 5)
        )).isInstanceOf(EntityNotFoundException.class);
    }

    private Question question(Category category) {
        Question question = new Question();
        question.setId(UUID.randomUUID());
        question.setCategory(category);
        question.setStatement("Pergunta de teste");
        question.setDifficulty("EASY");
        question.setActive(true);
        return question;
    }
}
