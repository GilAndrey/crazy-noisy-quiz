package com.crazynoisyquiz.backend.match.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerScoreCalculatorTest {
    private final AnswerScoreCalculator calculator = new AnswerScoreCalculator();
    private final Instant startedAt = Instant.parse("2026-10-08T12:00:00Z");

    // Testamos o limite e um nanossegundo depois para não perder as frações de segundo.
    @ParameterizedTest
    @CsvSource({
            "0, 100",
            "1000000000, 100",
            "1000000001, 80",
            "1900000000, 80",
            "3000000000, 80",
            "3000000001, 60",
            "5000000000, 60",
            "5000000001, 40",
            "8000000000, 40",
            "8000000001, 20",
            "9999999999, 20",
            "10000000000, 0",
            "10000000001, 0",
            "-1, 0"
    })
    void shouldScoreAccordingToExactElapsedTime(long elapsedNanos, int expectedPoints) {
        int points = calculator.calculate(true, startedAt, startedAt.plusNanos(elapsedNanos), 10);
        assertThat(points).isEqualTo(expectedPoints);
    }

    @Test
    void shouldGiveZeroForWrongAnswerEvenWhenImmediate() {
        assertThat(calculator.calculate(false, startedAt, startedAt, 10)).isZero();
    }

    @Test
    void shouldGiveZeroWhenTimestampIsMissing() {
        assertThat(calculator.calculate(true, null, startedAt, 10)).isZero();
        assertThat(calculator.calculate(true, startedAt, null, 10)).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldGiveZeroForInvalidTimeLimit(int timeLimit) {
        assertThat(calculator.calculate(true, startedAt, startedAt, timeLimit)).isZero();
    }

    @Test
    void shouldRespectShorterQuestionDeadlineBeforeApplyingScoreBands() {
        // Uma pergunta com prazo de 3 segundos já encerrou exatamente nesse instante.
        assertThat(calculator.calculate(true, startedAt, startedAt.plusNanos(2999999999L), 3))
                .isEqualTo(80);
        assertThat(calculator.calculate(true, startedAt, startedAt.plusSeconds(3), 3)).isZero();
    }

    @Test
    void shouldUseLastBandWhileWithinLongerQuestionDeadline() {
        assertThat(calculator.calculate(true, startedAt, startedAt.plusSeconds(11), 15))
                .isEqualTo(20);
    }
}
