package com.crazynoisyquiz.backend.user.service;

import com.crazynoisyquiz.backend.shared.exception.ResourceConflictException;
import com.crazynoisyquiz.backend.user.dto.CreateUserRequest;
import com.crazynoisyquiz.backend.user.dto.UserResponse;
import com.crazynoisyquiz.backend.user.model.AvatarKey;
import com.crazynoisyquiz.backend.user.model.User;
import com.crazynoisyquiz.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.argThat;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    // O service recebe versões simuladas das dependências,
    // então este teste não precisa acessar o banco de dados.
    @InjectMocks
    private UserService userService;

    @Test
    void shouldCreateUserSuccessfully() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("gil");
        request.setEmail("gil@email.com");
        request.setPassword("senha123");

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .username("gil")
                .email("gil@email.com")
                .passwordHash("hash-da-senha")
                .build();

        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(false);

        when(userRepository.existsByUsername(request.getUsername()))
                .thenReturn(false);

        when(passwordEncoder.encode(request.getPassword()))
                .thenReturn("hash-da-senha");

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);


        UserResponse response = userService.create(request);
        
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("gil");
        assertThat(response.getEmail()).isEqualTo("gil@email.com");

        verify(passwordEncoder).encode("senha123");
        verify(userRepository).save(any(User.class));

    }

    @Test
    void shouldCreateUserWithSelectedAvatar() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("gil");
        request.setEmail("gil@email.com");
        request.setPassword("senha123");
        request.setAvatarKey(AvatarKey.AVATAR_01);

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash("hash-da-senha")
                .avatarKey(AvatarKey.AVATAR_01)
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hash-da-senha");
        when(userRepository.save(argThat(user -> user.getAvatarKey() == AvatarKey.AVATAR_01)))
                .thenReturn(savedUser);

        UserResponse response = userService.create(request);

        // Confirma que o avatar escolhido foi persistido e também voltou na resposta.
        assertThat(response.getAvatarKey()).isEqualTo(AvatarKey.AVATAR_01);
        verify(userRepository).save(argThat(user -> user.getAvatarKey() == AvatarKey.AVATAR_01));
    }

    @Test
    void shouldCreateUserWithoutAvatarWhenNoneWasSelected() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("gil");
        request.setEmail("gil@email.com");
        request.setPassword("senha123");

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash("hash-da-senha")
                .avatarKey(null)
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hash-da-senha");
        when(userRepository.save(argThat(user -> user.getAvatarKey() == null))).thenReturn(savedUser);

        UserResponse response = userService.create(request);

        // Avatar é opcional: sem escolha, o perfil continua sendo criado normalmente.
        assertThat(response.getAvatarKey()).isNull();
        verify(userRepository).save(argThat(user -> user.getAvatarKey() == null));
    }

    @Test
    void shouldNotCreateUserWhenEmailAlreadyExists() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("gil");
        request.setEmail("gil@email.com");
        request.setPassword("senha123");

        // O repository encontrou alguém usando o mesmo email.
        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(true);

        // O service deve interromper o fluxo antes de tentar salvar.
        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("E-mail já cadastrado");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldNotCreateUserWhenUsernameAlreadyExists() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("gil");
        request.setEmail("outro@email.com");
        request.setPassword("senha123");

        // O email está disponível, mas o username já pertence a outro usuário.
        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(false);

        when(userRepository.existsByUsername(request.getUsername()))
                .thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Nome de usuário já cadastrado");

        // Nenhum usuário deve ser salvo quando existe conflito.
        verify(userRepository, never()).save(any(User.class));
    }
}
