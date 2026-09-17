package com.crazynoisyquiz.backend.shared.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;


// Dto para deixar mais clean o GlobalExceptionHandler
@Getter
@Builder
@AllArgsConstructor
public class ApiErrorResponse {

    private int status;
    private String message;
    private Map<String, String> errors;
    private Instant timestamp;

}
