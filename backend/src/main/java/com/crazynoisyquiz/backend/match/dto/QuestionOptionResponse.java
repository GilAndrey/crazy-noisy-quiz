package com.crazynoisyquiz.backend.match.dto;

import java.util.UUID;

public record QuestionOptionResponse(
        UUID id,
        String optionText,
        Integer optionOrder
) {
}
