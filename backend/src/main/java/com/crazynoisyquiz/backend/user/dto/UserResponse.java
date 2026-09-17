package com.crazynoisyquiz.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private String avatarUrl;
    private Instant createdAt;

}
