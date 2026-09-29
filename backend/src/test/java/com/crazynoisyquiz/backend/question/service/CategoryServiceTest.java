package com.crazynoisyquiz.backend.question.service;

import com.crazynoisyquiz.backend.question.model.Category;
import com.crazynoisyquiz.backend.question.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void shouldReturnActiveCategoriesFromRepository() {
        List<Category> categories = List.of(
                new Category(
                        UUID.randomUUID(),
                        "Ciência",
                        true,
                        Instant.now()
                ),
                new Category(
                        UUID.randomUUID(),
                        "História",
                        true,
                        Instant.now()
                )
        );

        when(categoryRepository.findAllByActiveTrueOrderByNameAsc())
                .thenReturn(categories);

        // O service deve repassar as categorias retornadas pelo repository.
        List<Category> result = categoryService.findActiveCategories();

        assertThat(result).containsExactlyElementsOf(categories);
        verify(categoryRepository).findAllByActiveTrueOrderByNameAsc();
    }

    @Test
    void shouldReturnEmptyListWhenThereAreNoActiveCategories() {
        when(categoryRepository.findAllByActiveTrueOrderByNameAsc())
                .thenReturn(List.of());

        // Sem categorias ativas, a resposta é uma lista vazia, não um erro.
        List<Category> result = categoryService.findActiveCategories();

        assertThat(result).isEmpty();
        verify(categoryRepository).findAllByActiveTrueOrderByNameAsc();
    }
}
