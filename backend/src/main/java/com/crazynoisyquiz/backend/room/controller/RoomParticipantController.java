package com.crazynoisyquiz.backend.room.controller;

import com.crazynoisyquiz.backend.room.dto.RoomParticipantResponse;
import com.crazynoisyquiz.backend.room.service.RoomParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/rooms")
@RequiredArgsConstructor
public class RoomParticipantController {

    private final RoomParticipantService roomParticipantService;

    @PostMapping("/{code}/join")
    public ResponseEntity<RoomParticipantResponse> joinRoom(
            @PathVariable String code,
            Authentication authentication
    ) {
        RoomParticipantResponse response =
                roomParticipantService.joinRoom(
                        code,
                        authentication.getName()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
