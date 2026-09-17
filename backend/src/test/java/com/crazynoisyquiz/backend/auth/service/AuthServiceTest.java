package com.crazynoisyquiz.backend.auth.service;

import com.crazynoisyquiz.backend.auth.dto.LoginRequest;
import com.crazynoisyquiz.backend.auth.dto.LoginResponse;
import com.crazynoisyquiz.backend.auth.exception.InvalidCredentialsException;
import com.crazynoisyquiz.backend.user.model.User;
import com.crazynoisyquiz.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // Simula a configuração que normalmente viria do application.yml.
        ReflectionTestUtils.setField(authService, "jwtExpiration", 3600000L);
    }

    @Test
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest();
        request.setEmail("gil@email.com");
        request.setPassword("senha123");

        User user = User.builder()
                .id(UUID.randomUUID())
                .email(request.getEmail())
                .username("gil")
                .passwordHash("senha-hash")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), "senha-hash")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("token-de-teste");

        LoginResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("token-de-teste");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUsername()).isEqualTo("gil");
        assertThat(response.getExpiresIn()).isEqualTo(3600000L);

        verify(jwtService).generateToken(user);
    }

    @Test
    void shouldRejectLoginWhenEmailDoesNotExist() {
        LoginRequest request = new LoginRequest();
        request.setEmail("inexistente@email.com");
        request.setPassword("senha123");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("E-mail ou senha inválidos");

        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void shouldRejectLoginWhenPasswordIsIncorrect() {
        LoginRequest request = new LoginRequest();
        request.setEmail("gil@email.com");
        request.setPassword("senha-errada");

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash("senha-hash")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), "senha-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("E-mail ou senha inválidos");

        verify(jwtService, never()).generateToken(any());
    }
}
