package org.wespeak.lesson.validator;

import org.springframework.stereotype.Component;
import org.wespeak.lesson.entity.Exercise;

import java.util.*;

/**
 * Validator for Match Pairs exercises.
 * Expects userAnswer: {"pairs": [{"left": "1", "right": "a"}, ...]}
 * Expects correctAnswer: {"pairs": [{"left": "1", "right": "a"}, ...]}
 */
@Component
public class MatchPairsAnswerValidator implements AnswerValidator {

    @Override
    public boolean supports(Exercise.ExerciseType type) {
        return type == Exercise.ExerciseType.match_pairs;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ValidationResult validate(Map<String, Object> userAnswer, Map<String, Object> correctAnswer) {
        List<Map<String, String>> userPairs = (List<Map<String, String>>) userAnswer.get("pairs");
        List<Map<String, String>> correctPairs = (List<Map<String, String>>) correctAnswer.get("pairs");

        if (userPairs == null || userPairs.isEmpty()) {
            return ValidationResult.incorrect("Please match all pairs.");
        }

        // Convert to sets for comparison (order doesn't matter)
        Set<String> userPairSet = new HashSet<>();
        for (Map<String, String> pair : userPairs) {
            userPairSet.add(pair.get("left") + ":" + pair.get("right"));
        }

        Set<String> correctPairSet = new HashSet<>();
        for (Map<String, String> pair : correctPairs) {
            correctPairSet.add(pair.get("left") + ":" + pair.get("right"));
        }

        if (userPairSet.equals(correctPairSet)) {
            return ValidationResult.correct("All pairs matched correctly!");
        }

        // Count correct matches
        long correctCount = userPairSet.stream()
            .filter(correctPairSet::contains)
            .count();

        return ValidationResult.incorrect(
            String.format("You got %d out of %d pairs correct.", correctCount, correctPairSet.size()));
    }
}
