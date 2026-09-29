package com.crazynoisyquiz.backend.question.repository;

import com.crazynoisyquiz.backend.question.model.Category;
import com.crazynoisyquiz.backend.question.model.Question;
import com.crazynoisyquiz.backend.question.model.QuestionOption;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class QuestionRepositoryTest {

    @Container
    private static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionOptionRepository questionOptionRepository;

    // O teste usa um Postgres temporário, sem alterar o banco local de desenvolvimento.
    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void shouldFindOnlyActiveQuestionsFromRequestedCategory() {
        Category category = saveCategory("Categoria de teste ciência");
        Category anotherCategory = saveCategory("Categoria de teste história");

        Question activeQuestion = saveQuestion(category, "Pergunta ativa", true);
        saveQuestion(category, "Pergunta inativa", false);
        saveQuestion(anotherCategory, "Pergunta de outra categoria", true);

        // A busca precisa respeitar ao mesmo tempo a categoria e o estado ativo.
        List<Question> result = questionRepository
                .findAllByCategoryIdAndActiveTrue(category.getId());

        assertThat(result)
                .extracting(Question::getId)
                .containsExactly(activeQuestion.getId());
    }

    @Test
    void shouldReturnQuestionOptionsInTheirConfiguredOrder() {
        Category category = saveCategory("Categoria de teste opções");
        Question question = saveQuestion(category, "Pergunta com opções", true);

        saveOption(question, "Terceira opção", 3);
        saveOption(question, "Primeira opção", 1);
        saveOption(question, "Segunda opção", 2);

        // A ordem retornada deve vir do banco, não da ordem de inserção.
        List<QuestionOption> result = questionOptionRepository
                .findAllByQuestionIdOrderByOptionOrderAsc(question.getId());

        assertThat(result)
                .extracting(QuestionOption::getOptionOrder)
                .containsExactly(1, 2, 3);
    }

    private Category saveCategory(String name) {
        Category category = new Category();
        category.setName(name);
        category.setActive(true);
        return categoryRepository.saveAndFlush(category);
    }

    private Question saveQuestion(Category category, String statement, boolean active) {
        Question question = new Question();
        question.setCategory(category);
        question.setStatement(statement);
        question.setDifficulty("EASY");
        question.setTimeLimitSeconds(10);
        question.setActive(active);
        return questionRepository.saveAndFlush(question);
    }

    private void saveOption(Question question, String text, int order) {
        QuestionOption option = new QuestionOption();
        option.setQuestion(question);
        option.setOptionText(text);
        option.setOptionOrder(order);
        questionOptionRepository.save(option);
    }
}
