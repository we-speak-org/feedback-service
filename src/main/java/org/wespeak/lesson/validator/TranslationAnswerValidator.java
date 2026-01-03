package org.wespeak.lesson.validator;

import org.springframework.stereotype.Component;
import org.wespeak.lesson.entity.Exercise;

import java.util.List;
import java.util.Map;

/**
 * Validator for Translation exercises.
 * Expects userAnswer: {"text": "Hello, how are you?"}
 * Expects correctAnswer: {"text": "Hello, how are you?", "alternatives": [...]}
 */
@Component
public class TranslationAnswerValidator implements AnswerValidator {

    @Override
    public boolean supports(Exercise.ExerciseType type) {
        return type == Exercise.ExerciseType.translation;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ValidationResult validate(Map<String, Object> userAnswer, Map<String, Object> correctAnswer) {
        String userText = normalizeTranslation((String) userAnswer.get("text"));
        String correctText = normalizeTranslation((String) correctAnswer.get("text"));

        if (userText == null || userText.isBlank()) {
            return ValidationResult.incorrect("Please enter a translation.");
        }

        // Check main answer
        if (userText.equalsIgnoreCase(correctText)) {
            return ValidationResult.correct("Excellent translation!");
        }

        // Check alternatives
        Object alternativesObj = correctAnswer.get("alternatives");
        if (alternativesObj instanceof List) {
            List<String> alternatives = (List<String>) alternativesObj;
            for (String alt : alternatives) {
                if (userText.equalsIgnoreCase(normalizeTranslation(alt))) {
                    return ValidationResult.correct("Good translation! An alternative is: " + correctText);
                }
            }
        }

        return ValidationResult.incorrect("Not quite. A correct translation is: " + correctText);
    }

    private String normalizeTranslation(String text) {
        if (text == null) return "";
        // Remove extra spaces, normalize punctuation
        return text.trim()
            .replaceAll("\\s+", " ")
            .replaceAll("[.!?]+$", "");
    }
}
