package com.crazynoisyquiz.backend.room.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class RoomParticipantResponse {

    private UUID id;
    private UUID roomId;
    private UUID userId;
    private String roomCode;
    private Instant joinedAt;
}
