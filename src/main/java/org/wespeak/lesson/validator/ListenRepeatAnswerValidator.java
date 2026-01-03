package org.wespeak.lesson.validator;

import org.springframework.stereotype.Component;
import org.wespeak.lesson.entity.Exercise;

import java.util.Map;

/**
 * Validator for Listen and Repeat exercises.
 * For now, this is a simple validator that accepts the answer.
 * In production, this would integrate with Speech-to-Text services.
 */
@Component
public class ListenRepeatAnswerValidator implements AnswerValidator {

    @Override
    public boolean supports(Exercise.ExerciseType type) {
        return type == Exercise.ExerciseType.listen_repeat;
    }

    @Override
    public ValidationResult validate(Map<String, Object> userAnswer, Map<String, Object> correctAnswer) {
        // For listen_repeat, we typically need STT processing
        // For now, we accept the submission
        String userText = (String) userAnswer.get("text");
        String expectedText = (String) correctAnswer.get("text");

        if (userText == null || userText.isBlank()) {
            // If no text, check if audio was submitted
            Boolean audioSubmitted = (Boolean) userAnswer.get("audioSubmitted");
            if (Boolean.TRUE.equals(audioSubmitted)) {
                return ValidationResult.correct("Audio received. Great practice!");
            }
            return ValidationResult.incorrect("Please record your pronunciation.");
        }

        // Simple text comparison (in production, use phonetic comparison)
        if (userText.equalsIgnoreCase(expectedText)) {
            return ValidationResult.correct("Excellent pronunciation!");
        }

        return ValidationResult.correct("Good effort! Keep practicing.");
    }
}
