package com.crazynoisyquiz.backend.match.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class AnswerScoreCalculator {

    public int calculate(
            boolean correct,
            Instant startedAt,
            Instant answeredAt,
            int timeLimitSeconds
    ) {

        if (!correct || startedAt == null || answeredAt == null || timeLimitSeconds <= 0) {
            return 0;
        }

        Duration elapsed = Duration.between(startedAt, answeredAt);
        Duration timeLimit = Duration.ofSeconds(timeLimitSeconds);

        if (elapsed.isNegative() || elapsed.compareTo(timeLimit) >= 0) {
            return 0;
        }

        if (elapsed.compareTo(Duration.ofSeconds(1)) <= 0) {
            return 100;
        }

        if (elapsed.compareTo(Duration.ofSeconds(3)) <= 0) {
            return 80;
        }

        if (elapsed.compareTo(Duration.ofSeconds(5)) <= 0) {
            return 60;
        }

        if (elapsed.compareTo(Duration.ofSeconds(8)) <= 0) {
            return 40;
        }
        return 20;
    }
}
