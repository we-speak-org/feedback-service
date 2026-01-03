package org.wespeak.lesson.validator;

import org.wespeak.lesson.entity.Exercise;

import java.util.Map;

public interface AnswerValidator {
    boolean supports(Exercise.ExerciseType type);
    ValidationResult validate(Map<String, Object> userAnswer, Map<String, Object> correctAnswer);
}
