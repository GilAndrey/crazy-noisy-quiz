package com.crazynoisyquiz.backend.question.controller;

import com.crazynoisyquiz.backend.auth.service.JwtService;
import com.crazynoisyquiz.backend.question.model.Category;
import com.crazynoisyquiz.backend.question.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Simulamos o service para testar o controller sem acessar banco de dados.
    @MockitoBean
    private CategoryService categoryService;

    // O filtro JWT ainda é criado no contexto e precisa dessa dependência simulada.
    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldReturnActiveCategoriesAndStatus200() throws Exception {
        Instant createdAt = Instant.parse("2026-09-26T02:14:39Z");
        List<Category> categories = List.of(
                new Category(UUID.randomUUID(), "Ciência", true, createdAt),
                new Category(UUID.randomUUID(), "História", true, createdAt)
        );

        when(categoryService.findActiveCategories()).thenReturn(categories);

        // Fazemos a chamada HTTP simulada e conferimos o formato da resposta.
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ciência"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[1].name").value("História"));

        // Confirma que o controller pediu a lista ao service.
        verify(categoryService).findActiveCategories();
    }
}
