package org.wespeak.lesson.validator;

import org.springframework.stereotype.Component;
import org.wespeak.lesson.entity.Exercise;

import java.util.List;
import java.util.Map;

/**
 * Validator for Ordering exercises.
 * Expects userAnswer: {"order": ["word3", "word1", "word2"]}
 * Expects correctAnswer: {"order": ["word1", "word2", "word3"]}
 */
@Component
public class OrderingAnswerValidator implements AnswerValidator {

    @Override
    public boolean supports(Exercise.ExerciseType type) {
        return type == Exercise.ExerciseType.ordering;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ValidationResult validate(Map<String, Object> userAnswer, Map<String, Object> correctAnswer) {
        List<String> userOrder = (List<String>) userAnswer.get("order");
        List<String> correctOrder = (List<String>) correctAnswer.get("order");

        if (userOrder == null || userOrder.isEmpty()) {
            return ValidationResult.incorrect("Please arrange the items in order.");
        }

        if (userOrder.equals(correctOrder)) {
            return ValidationResult.correct("Perfect order!");
        }

        // Count items in correct position
        int correctPositions = 0;
        int minLen = Math.min(userOrder.size(), correctOrder.size());
        for (int i = 0; i < minLen; i++) {
            if (userOrder.get(i).equals(correctOrder.get(i))) {
                correctPositions++;
            }
        }

        return ValidationResult.incorrect(
            String.format("Not quite right. %d out of %d items are in the correct position.",
                correctPositions, correctOrder.size()));
    }
}
