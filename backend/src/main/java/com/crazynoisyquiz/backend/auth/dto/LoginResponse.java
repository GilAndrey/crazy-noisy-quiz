package com.crazynoisyquiz.backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String tokenType;
    private long expiresIn;
    private UUID userId;
    private String username;

}
