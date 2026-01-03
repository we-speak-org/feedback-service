package org.wespeak.lesson.exception;

import lombok.Getter;

@Getter
public class MaxAttemptsReachedException extends RuntimeException {
    private final String code = "MAX_ATTEMPTS_REACHED";
    private final String exerciseId;
    private final Integer maxAttempts;

    public MaxAttemptsReachedException(String exerciseId, Integer maxAttempts) {
        super(String.format("Maximum attempts (%d) reached for this exercise. Please restart the lesson.", maxAttempts));
        this.exerciseId = exerciseId;
        this.maxAttempts = maxAttempts;
    }
}
