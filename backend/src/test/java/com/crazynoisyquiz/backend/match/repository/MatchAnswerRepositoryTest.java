package com.crazynoisyquiz.backend.match.repository;

import com.crazynoisyquiz.backend.match.model.*;
import com.crazynoisyquiz.backend.question.model.QuestionOption;
import com.crazynoisyquiz.backend.room.model.QuizRoom;
import com.crazynoisyquiz.backend.user.model.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MatchAnswerRepositoryTest {
    // Usa um Postgres temporário; não mexe nas respostas do banco de desenvolvimento.
    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private EntityManager entityManager;
    @Autowired private MatchAnswerRepository repository;
    private MatchRound round;
    private MatchParticipant participant;
    private QuestionOption option;

    @BeforeEach
    void setUp() {
        User user = User.builder().username("answer-player")
                .email("answer-player@email.com").passwordHash("test-hash").build();
        entityManager.persist(user);
        QuizRoom room = QuizRoom.builder().code("ANS123").owner(user).build();
        entityManager.persist(room);
        Match match = Match.builder().room(room).totalRounds(5).currentRoundNumber(1).build();
        entityManager.persist(match);
        participant = MatchParticipant.builder().match(match).user(user).build();
        entityManager.persist(participant);
        // A alternativa e a pergunta são carregadas do seed aplicado pelo Flyway.
        option = entityManager.find(QuestionOption.class,
                UUID.fromString("30000000-0000-0000-0000-000000000001"));
        round = MatchRound.builder().match(match).question(option.getQuestion())
                .roundNumber(1).status(RoundStatus.IN_PROGRESS).build();
        entityManager.persist(round);
        entityManager.flush();
    }

    private MatchAnswer answer() {
        return MatchAnswer.builder().round(round).participant(participant).option(option).build();
    }

    @Test
    void shouldPersistAnswerAndFindParticipantSubmission() {
        assertThat(repository.existsByRoundIdAndParticipantId(round.getId(), participant.getId())).isFalse();
        MatchAnswer saved = repository.saveAndFlush(answer());
        UUID answerId = saved.getId();
        entityManager.clear();

        MatchAnswer loaded = repository.findById(answerId).orElseThrow();
        assertThat(loaded.getAnsweredAt()).isNotNull();
        assertThat(loaded.getRound().getId()).isEqualTo(round.getId());
        assertThat(loaded.getParticipant().getId()).isEqualTo(participant.getId());
        assertThat(loaded.getOption().getId()).isEqualTo(option.getId());
        assertThat(repository.existsByRoundIdAndParticipantId(round.getId(), participant.getId())).isTrue();
    }

    @Test
    void shouldEnforceOneAnswerPerRoundAndParticipantInDatabase() {
        repository.saveAndFlush(answer());
        // A proteção vale no banco mesmo se alguém ignorar a verificação do service.
        assertThatThrownBy(() -> repository.saveAndFlush(answer()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("uq_match_answers_round_participant");
    }
}
