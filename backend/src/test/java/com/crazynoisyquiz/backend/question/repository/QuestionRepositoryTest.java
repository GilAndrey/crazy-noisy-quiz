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
    void shouldFindActiveQuestionsAcrossRequestedCategoriesWithoutInactiveOnes() {
        Category firstCategory = saveCategory("Categoria para partida A");
        Category secondCategory = saveCategory("Categoria para partida B");
        Category anotherCategory = saveCategory("Categoria fora da seleção");

        Question firstQuestion = saveQuestion(firstCategory, "Pergunta A", true);
        Question secondQuestion = saveQuestion(secondCategory, "Pergunta B", true);
        saveQuestion(secondCategory, "Pergunta inativa", false);
        saveQuestion(anotherCategory, "Pergunta não selecionada", true);

        // O sorteio recebe somente perguntas ativas das categorias escolhidas.
        List<Question> result = questionRepository.findAllByCategoryIdInAndActiveTrue(
                List.of(firstCategory.getId(), secondCategory.getId())
        );

        assertThat(result)
                .extracting(Question::getId)
                .containsExactlyInAnyOrder(firstQuestion.getId(), secondQuestion.getId());
    }

    @Test
    void shouldFindOnlyActiveCategoriesFromRequestedIds() {
        Category activeCategory = saveCategory("Categoria ativa selecionada");
        Category inactiveCategory = saveCategory("Categoria inativa selecionada");
        inactiveCategory.setActive(false);
        categoryRepository.saveAndFlush(inactiveCategory);
        Category unselectedCategory = saveCategory("Categoria não selecionada");

        // Categorias inativas ou não pedidas não podem ser usadas para montar a partida.
        List<Category> result = categoryRepository.findAllByIdInAndActiveTrue(
                List.of(activeCategory.getId(), inactiveCategory.getId())
        );

        assertThat(result)
                .extracting(Category::getId)
                .containsExactly(activeCategory.getId());
        assertThat(result).doesNotContain(unselectedCategory);
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
