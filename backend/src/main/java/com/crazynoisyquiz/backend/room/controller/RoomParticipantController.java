package com.crazynoisyquiz.backend.room.controller;

import com.crazynoisyquiz.backend.room.dto.RoomParticipantResponse;
import com.crazynoisyquiz.backend.room.service.RoomParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

    @DeleteMapping("/{code}/leave")
    public ResponseEntity<Void> leaveRoom(
            @PathVariable String code,
            Authentication authentication
    ) {
        roomParticipantService.leaveRoom(
                code, authentication.getName()
        );
        return ResponseEntity.noContent().build();
    }
}
