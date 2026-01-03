package org.wespeak.lesson.exception;

import lombok.Getter;

@Getter
public class InvalidAnswerException extends RuntimeException {
    private final String code = "INVALID_ANSWER";
    private final String exerciseId;
    private final String reason;

    public InvalidAnswerException(String exerciseId, String reason) {
        super(String.format("Invalid answer format for exercise %s: %s", exerciseId, reason));
        this.exerciseId = exerciseId;
        this.reason = reason;
    }
}
