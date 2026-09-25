package com.crazynoisyquiz.backend.room.controller;

import com.crazynoisyquiz.backend.room.dto.CreateRoomRequest;
import com.crazynoisyquiz.backend.room.dto.RoomResponse;
import com.crazynoisyquiz.backend.room.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    // Cria uma sala com o usuario autenticado como o dono da sala!!!
    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(
            @Valid @RequestBody(required = false) CreateRoomRequest request,
            Authentication authentication
    ) {
        RoomResponse response = roomService.create(
                request,
                authentication.getName()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{code}")
    public ResponseEntity<RoomResponse> findRoomByCode(
            @PathVariable String code
    ) {
        RoomResponse response = roomService.findByCode(code);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}
