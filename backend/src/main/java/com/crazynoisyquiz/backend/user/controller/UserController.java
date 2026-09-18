package com.crazynoisyquiz.backend.user.controller;

import com.crazynoisyquiz.backend.user.dto.CreateUserRequest;
import com.crazynoisyquiz.backend.user.dto.UserResponse;
import com.crazynoisyquiz.backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping                          // @Valid é para validar com as regras colocadas.
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> currentUser(
            @AuthenticationPrincipal String email
    ) {
        UserResponse response = userService.getCurrentUser(email);
        return ResponseEntity.ok(response);
    }
}
