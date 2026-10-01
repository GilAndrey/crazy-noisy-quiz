package com.crazynoisyquiz.backend.match.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record StartMatchRequest(
        @NotEmpty List<@NotNull UUID> categoryIds,

        @NotNull
        @Min(5)
        @Max(20)
        Integer totalRounds
        )
{ }
