package com.crazynoisyquiz.backend.auth.controller;

import com.crazynoisyquiz.backend.auth.dto.LoginResponse;
import com.crazynoisyquiz.backend.auth.exception.InvalidCredentialsException;
import com.crazynoisyquiz.backend.auth.service.AuthService;
import com.crazynoisyquiz.backend.auth.service.JwtService;
import com.crazynoisyquiz.backend.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.context.annotation.Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    // O filtro JWT é criado pelo contexto, então simulamos sua dependência.
    // Os filtros estão desligados neste teste porque aqui o foco é o controller.
    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldLoginAndReturnToken() throws Exception {
        LoginResponse response = LoginResponse.builder()
                .token("token-de-teste")
                .tokenType("Bearer")
                .expiresIn(3600000L)
                .userId(UUID.randomUUID())
                .username("gil")
                .build();

        when(authService.login(any())).thenReturn(response);

        // Aqui testamos o contrato HTTP: uma requisição válida deve devolver o token.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"gil@email.com\",\"password\":\"senha123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-de-teste"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("gil"));
    }

    @Test
    void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
        when(authService.login(any()))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"gil@email.com\",\"password\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos"));
    }

    @Test
    void shouldReturn400WhenLoginRequestIsInvalid() throws Exception {
        // A validação acontece antes do service, então não precisamos simular o banco.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"email-invalido\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }
}
