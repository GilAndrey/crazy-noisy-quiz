package com.crazynoisyquiz.backend.room.dto;

import com.crazynoisyquiz.backend.room.model.RoomStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class RoomResponse {

    private UUID id;
    private String code;
    private UUID ownerId;
    private RoomStatus status;
    private Integer maxPlayers;
    private Instant createdAt;

}
