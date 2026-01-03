package org.wespeak.lesson.validator;

import org.springframework.stereotype.Component;
import org.wespeak.lesson.entity.Exercise;

import java.util.Map;

/**
 * Validator for Multiple Choice Questions.
 * Expects userAnswer: {"optionId": "a"}
 * Expects correctAnswer: {"optionId": "a"}
 */
@Component
public class McqAnswerValidator implements AnswerValidator {

    @Override
    public boolean supports(Exercise.ExerciseType type) {
        return type == Exercise.ExerciseType.mcq;
    }

    @Override
    public ValidationResult validate(Map<String, Object> userAnswer, Map<String, Object> correctAnswer) {
        String userOption = (String) userAnswer.get("optionId");
        String correctOption = (String) correctAnswer.get("optionId");

        if (userOption == null) {
            return ValidationResult.incorrect("Please select an option.");
        }

        if (userOption.equalsIgnoreCase(correctOption)) {
            return ValidationResult.correct("Correct! Well done.");
        } else {
            String correctText = (String) correctAnswer.getOrDefault("text", "");
            return ValidationResult.incorrect("Not quite. The correct answer is: " + correctText);
        }
    }
}
