package com.crazynoisyquiz.backend.room.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateRoomRequest {

    @Min(value = 2, message = "A sala deve ter no minimo 2 jogadores")
    @Max(value = 8, message = "A sala pode ter no máximo 8 jogadores")
    private Integer maxPlayers;
}
