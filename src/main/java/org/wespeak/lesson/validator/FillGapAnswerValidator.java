package org.wespeak.lesson.validator;

import org.springframework.stereotype.Component;
import org.wespeak.lesson.entity.Exercise;

import java.util.List;
import java.util.Map;

/**
 * Validator for Fill in the Gap exercises.
 * Expects userAnswer: {"text": "meet"}
 * Expects correctAnswer: {"text": "meet", "alternatives": ["Meet", "MEET"]}
 */
@Component
public class FillGapAnswerValidator implements AnswerValidator {

    @Override
    public boolean supports(Exercise.ExerciseType type) {
        return type == Exercise.ExerciseType.fill_gap;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ValidationResult validate(Map<String, Object> userAnswer, Map<String, Object> correctAnswer) {
        String userText = normalizeText((String) userAnswer.get("text"));
        String correctText = normalizeText((String) correctAnswer.get("text"));

        if (userText == null || userText.isBlank()) {
            return ValidationResult.incorrect("Please enter an answer.");
        }

        // Check main answer
        if (userText.equalsIgnoreCase(correctText)) {
            return ValidationResult.correct("Correct! Well done.");
        }

        // Check alternatives
        Object alternativesObj = correctAnswer.get("alternatives");
        if (alternativesObj instanceof List) {
            List<String> alternatives = (List<String>) alternativesObj;
            for (String alt : alternatives) {
                if (userText.equalsIgnoreCase(normalizeText(alt))) {
                    return ValidationResult.correct("Correct! Well done.");
                }
            }
        }

        return ValidationResult.incorrect("Not quite. The correct answer is: " + correctText);
    }

    private String normalizeText(String text) {
        return text != null ? text.trim() : "";
    }
}
