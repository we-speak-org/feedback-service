package org.wespeak.lesson.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class LessonLockedException extends RuntimeException {
    private final String code = "LESSON_LOCKED";
    private final String lessonId;
    private final String requiredLessonId;
    private final Integer requiredScore;

    public LessonLockedException(String lessonId, String requiredLessonId, Integer requiredScore) {
        super("This lesson is not yet unlocked. Complete the previous lesson first.");
        this.lessonId = lessonId;
        this.requiredLessonId = requiredLessonId;
        this.requiredScore = requiredScore;
    }

    public Map<String, Object> getDetails() {
        return Map.of(
            "requiredLessonId", requiredLessonId != null ? requiredLessonId : "",
            "requiredScore", requiredScore != null ? requiredScore : 70
        );
    }
}
