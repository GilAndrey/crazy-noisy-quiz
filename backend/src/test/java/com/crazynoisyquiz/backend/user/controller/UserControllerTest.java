package com.crazynoisyquiz.backend.user.controller;

import com.crazynoisyquiz.backend.config.SecurityConfig;
import com.crazynoisyquiz.backend.user.dto.UserResponse;
import com.crazynoisyquiz.backend.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // O controller será testado isoladamente; o service fica simulado.
    @MockitoBean
    private UserService userService;

    @Test
    void shouldCreateUserAndReturn201() throws Exception {
        UUID userId = UUID.randomUUID();

        UserResponse userResponse = new UserResponse(
                userId,
                "gil",
                "gil@email.com",
                null,
                null
        );

        // Neste teste, fingimos que o service criou o usuário com sucesso.
        when(userService.create(any()))
                .thenReturn(userResponse);

        String requestBody = """
                {
                    "username": "gil",
                    "email": "gil@email.com",
                    "password": "senha123",
                    "avatarKey": null
                }
                """;

        // Enviamos uma requisição para o endpoint usando o MockMvc.
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("gil"))
                .andExpect(jsonPath("$.email").value("gil@email.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void shouldReturn400WhenRequestIsInvalid() throws Exception {
        String requestBody = """
                {
                    "username": "",
                    "email": "email-invalido",
                    "password": "123"
                }
                """;

        // A validação acontece no controller, antes de chamar o service.
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Falha de validação"))
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void shouldReturn400WhenAvatarKeyIsUnknown() throws Exception {
        String requestBody = """
                {
                    "username": "gil",
                    "email": "gil@email.com",
                    "password": "senha123",
                    "avatarKey": "AVATAR_99"
                }
                """;

        // O JSON não pode ser convertido para AvatarKey quando recebe uma opção inexistente.
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        // A requisição inválida deve ser barrada antes de chegar ao service.
        verifyNoInteractions(userService);
    }
}
