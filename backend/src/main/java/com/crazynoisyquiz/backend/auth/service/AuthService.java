package com.crazynoisyquiz.backend.auth.service;

import com.crazynoisyquiz.backend.auth.dto.LoginRequest;
import com.crazynoisyquiz.backend.auth.dto.LoginResponse;
import com.crazynoisyquiz.backend.auth.exception.InvalidCredentialsException;
import com.crazynoisyquiz.backend.user.model.User;
import com.crazynoisyquiz.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        // Comparar a senha recebida com o hash armazenado no banco!!!!! :)
        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash()
        )) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user);

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration)
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }
}
