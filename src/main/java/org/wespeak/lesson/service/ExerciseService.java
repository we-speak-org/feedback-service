package org.wespeak.lesson.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.wespeak.lesson.dto.ExerciseSubmissionResultDto;
import org.wespeak.lesson.dto.SubmitAnswerRequest;
import org.wespeak.lesson.entity.Exercise;
import org.wespeak.lesson.entity.Lesson;
import org.wespeak.lesson.exception.InvalidAnswerException;
import org.wespeak.lesson.exception.MaxAttemptsReachedException;
import org.wespeak.lesson.exception.ResourceNotFoundException;
import org.wespeak.lesson.repository.LessonRepository;
import org.wespeak.lesson.validator.AnswerValidatorFactory;
import org.wespeak.lesson.validator.ValidationResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExerciseService {

    private static final int MAX_ATTEMPTS_FREE = 3;
    private static final int MAX_ATTEMPTS_PREMIUM = 5;

    private final LessonRepository lessonRepository;
    private final AnswerValidatorFactory validatorFactory;

    // In-memory attempt tracking (in production, use Redis or DB)
    private final Map<String, Integer> attemptTracker = new ConcurrentHashMap<>();

    /**
     * Submit an answer to an exercise.
     */
    public ExerciseSubmissionResultDto submitAnswer(
            String exerciseId,
            String lessonId,
            String userId,
            SubmitAnswerRequest request) {

        // Find the lesson and exercise
        Lesson lesson = lessonRepository.findById(lessonId)
            .orElseThrow(() -> ResourceNotFoundException.lessonNotFound(lessonId));

        Exercise exercise = lesson.getExercises().stream()
            .filter(e -> e.getId().equals(exerciseId))
            .findFirst()
            .orElseThrow(() -> ResourceNotFoundException.exerciseNotFound(exerciseId));

        // Check attempts
        String attemptKey = userId + ":" + exerciseId;
        int currentAttempts = attemptTracker.getOrDefault(attemptKey, 0);
        int maxAttempts = MAX_ATTEMPTS_FREE; // TODO: Check user tier for premium

        if (currentAttempts >= maxAttempts) {
            throw new MaxAttemptsReachedException(exerciseId, maxAttempts);
        }

        // Validate the answer
        ValidationResult result;
        try {
            result = validatorFactory.validate(exercise.getType(), request.getAnswer(), exercise.getCorrectAnswer());
        } catch (Exception e) {
            throw new InvalidAnswerException(exerciseId, e.getMessage());
        }

        // Increment attempts
        int attemptNumber = currentAttempts + 1;
        attemptTracker.put(attemptKey, attemptNumber);

        // Calculate points
        int pointsEarned = result.isCorrect() ? exercise.getPoints() : 0;

        return ExerciseSubmissionResultDto.builder()
            .isCorrect(result.isCorrect())
            .pointsEarned(pointsEarned)
            .correctAnswer(exercise.getCorrectAnswer())
            .feedback(result.getFeedback())
            .attemptNumber(attemptNumber)
            .build();
    }

    /**
     * Reset attempts for a user starting a new lesson session.
     */
    public void resetAttempts(String userId, String lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson != null) {
            lesson.getExercises().forEach(ex -> {
                attemptTracker.remove(userId + ":" + ex.getId());
            });
        }
    }
}
