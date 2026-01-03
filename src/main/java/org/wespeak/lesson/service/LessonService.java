package org.wespeak.lesson.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.wespeak.lesson.dto.*;
import org.wespeak.lesson.entity.*;
import org.wespeak.lesson.exception.LessonLockedException;
import org.wespeak.lesson.exception.ResourceNotFoundException;
import org.wespeak.lesson.repository.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final UnitRepository unitRepository;
    private final CourseRepository courseRepository;
    private final LessonCompletionRepository lessonCompletionRepository;
    private final UserProgressRepository userProgressRepository;
    private final UnlockService unlockService;
    private final LessonEventPublisher eventPublisher;

    /**
     * Get lesson details with exercises (without answers).
     */
    public LessonDetailDto findLessonById(String lessonId, String userId) {
        Lesson lesson = lessonRepository.findById(lessonId)
            .orElseThrow(() -> ResourceNotFoundException.lessonNotFound(lessonId));

        Unit unit = unitRepository.findById(lesson.getUnitId()).orElse(null);
        
        boolean isUnlocked = unlockService.isLessonUnlocked(lessonId, userId);
        UserProgressStateDto progressState = getUserProgressState(lessonId, userId);

        List<ExerciseDto> exerciseDtos = lesson.getExercises().stream()
            .map(this::toExerciseDto)
            .collect(Collectors.toList());

        return LessonDetailDto.builder()
            .id(lesson.getId())
            .title(lesson.getTitle())
            .description(lesson.getDescription())
            .type(lesson.getType())
            .estimatedMinutes(lesson.getEstimatedMinutes())
            .xpReward(lesson.getXpReward())
            .unit(unit != null ? UnitRefDto.builder()
                .id(unit.getId())
                .title(unit.getTitle())
                .build() : null)
            .exercises(exerciseDtos)
            .isUnlocked(isUnlocked)
            .userProgress(progressState)
            .build();
    }

    /**
     * Start a lesson session.
     */
    public LessonSessionDto startLesson(String lessonId, String userId) {
        Lesson lesson = lessonRepository.findById(lessonId)
            .orElseThrow(() -> ResourceNotFoundException.lessonNotFound(lessonId));

        // Check if lesson is unlocked
        if (!unlockService.isLessonUnlocked(lessonId, userId)) {
            Optional<Lesson> previousLesson = unlockService.getPreviousLesson(lessonId);
            throw new LessonLockedException(
                lessonId,
                previousLesson.map(Lesson::getId).orElse(null),
                70
            );
        }

        String sessionId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now();

        // Publish lesson started event
        publishLessonStartedEvent(lesson, userId);

        return LessonSessionDto.builder()
            .sessionId(sessionId)
            .lessonId(lessonId)
            .startedAt(startedAt)
            .exerciseCount(lesson.getExercises().size())
            .build();
    }

    /**
     * Complete a lesson and record the score.
     */
    public LessonCompletionResultDto completeLesson(String lessonId, String userId, CompleteLessonRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
            .orElseThrow(() -> ResourceNotFoundException.lessonNotFound(lessonId));

        // Check if lesson is unlocked
        if (!unlockService.isLessonUnlocked(lessonId, userId)) {
            Optional<Lesson> previousLesson = unlockService.getPreviousLesson(lessonId);
            throw new LessonLockedException(
                lessonId,
                previousLesson.map(Lesson::getId).orElse(null),
                70
            );
        }

        // Calculate attempt number
        long attempts = lessonCompletionRepository.countByUserIdAndLessonId(userId, lessonId);
        int attemptNumber = (int) attempts + 1;

        // Calculate XP earned
        int xpEarned = calculateXP(lesson.getXpReward(), request.getScore(), attemptNumber);

        // Create completion record
        LessonCompletion completion = LessonCompletion.builder()
            .userId(userId)
            .lessonId(lessonId)
            .score(request.getScore())
            .xpEarned(xpEarned)
            .correctAnswers(request.getCorrectAnswers())
            .totalExercises(request.getTotalExercises())
            .timeSpentSeconds(request.getTimeSpentSeconds())
            .attemptNumber(attemptNumber)
            .completedAt(Instant.now())
            .build();
        lessonCompletionRepository.save(completion);

        // Update user progress
        updateUserProgress(userId, lesson, completion);

        // Find next lesson if unlocked
        LessonCompletionResultDto.LessonRefDto nextLessonRef = null;
        if (request.getScore() >= 70) {
            Optional<Lesson> nextLesson = unlockService.findNextLesson(lessonId);
            if (nextLesson.isPresent()) {
                nextLessonRef = LessonCompletionResultDto.LessonRefDto.builder()
                    .id(nextLesson.get().getId())
                    .title(nextLesson.get().getTitle())
                    .build();
            }
        }

        // Publish events
        publishLessonCompletedEvent(lesson, userId, completion, attemptNumber == 1);
        checkAndPublishUnitCompleted(lesson, userId);
        checkAndPublishCourseCompleted(lesson, userId);

        // Get updated progress
        UserProgress progress = userProgressRepository.findByUserIdAndTargetLanguageCode(
            userId, getLanguageCode(lesson)).orElse(null);

        return LessonCompletionResultDto.builder()
            .completion(LessonCompletionResultDto.CompletionDto.builder()
                .id(completion.getId())
                .lessonId(lessonId)
                .score(request.getScore())
                .xpEarned(xpEarned)
                .attemptNumber(attemptNumber)
                .completedAt(completion.getCompletedAt())
                .build())
            .unlocked(LessonCompletionResultDto.UnlockedDto.builder()
                .nextLesson(nextLessonRef)
                .build())
            .progress(LessonCompletionResultDto.ProgressStatsDto.builder()
                .lessonsCompleted(progress != null ? progress.getLessonsCompleted() : 1)
                .averageScore(progress != null ? progress.getAverageScore() : request.getScore())
                .build())
            .build();
    }

    /**
     * Calculate XP earned based on score and bonuses.
     */
    public int calculateXP(int baseXP, int score, int attemptNumber) {
        double xp = baseXP * (score / 100.0);
        
        // Bonus for excellent score (>= 90%)
        if (score >= 90) {
            xp *= 1.2;
        }
        
        // Bonus for first attempt success (>= 70%)
        if (attemptNumber == 1 && score >= 70) {
            xp *= 1.1;
        }
        
        return (int) Math.round(xp);
    }

    private void updateUserProgress(String userId, Lesson lesson, LessonCompletion completion) {
        String languageCode = getLanguageCode(lesson);
        
        UserProgress progress = userProgressRepository.findByUserIdAndTargetLanguageCode(userId, languageCode)
            .orElseGet(() -> UserProgress.builder()
                .userId(userId)
                .targetLanguageCode(languageCode)
                .lessonsCompleted(0)
                .averageScore(0)
                .totalTimeMinutes(0)
                .build());

        // Update stats
        int newLessonsCompleted = progress.getLessonsCompleted() + 1;
        int newTotalTime = progress.getTotalTimeMinutes() + (completion.getTimeSpentSeconds() / 60);
        int newAverageScore = ((progress.getAverageScore() * progress.getLessonsCompleted()) + completion.getScore()) / newLessonsCompleted;

        progress.setLessonsCompleted(newLessonsCompleted);
        progress.setAverageScore(newAverageScore);
        progress.setTotalTimeMinutes(newTotalTime);
        progress.setLastActivityAt(Instant.now());

        // Update current position
        Unit unit = unitRepository.findById(lesson.getUnitId()).orElse(null);
        if (unit != null) {
            progress.setCurrentCourseId(unit.getCourseId());
            progress.setCurrentUnitId(unit.getId());
            progress.setCurrentLessonId(lesson.getId());
        }

        userProgressRepository.save(progress);
    }

    private String getLanguageCode(Lesson lesson) {
        Unit unit = unitRepository.findById(lesson.getUnitId()).orElse(null);
        if (unit == null) return "en";
        Course course = courseRepository.findById(unit.getCourseId()).orElse(null);
        return course != null ? course.getTargetLanguageCode() : "en";
    }

    private ExerciseDto toExerciseDto(Exercise exercise) {
        return ExerciseDto.builder()
            .id(exercise.getId())
            .type(exercise.getType())
            .order(exercise.getOrder())
            .question(exercise.getQuestion())
            .hint(exercise.getHint())
            .audioUrl(exercise.getAudioUrl())
            .imageUrl(exercise.getImageUrl())
            .content(exercise.getContent())
            .points(exercise.getPoints())
            .build();
    }

    private UserProgressStateDto getUserProgressState(String lessonId, String userId) {
        if (userId == null) {
            return UserProgressStateDto.builder()
                .isCompleted(false)
                .bestScore(null)
                .attempts(0)
                .build();
        }

        Optional<LessonCompletion> bestCompletion = lessonCompletionRepository
            .findFirstByUserIdAndLessonIdOrderByScoreDesc(userId, lessonId);
        long attempts = lessonCompletionRepository.countByUserIdAndLessonId(userId, lessonId);

        return UserProgressStateDto.builder()
            .isCompleted(bestCompletion.isPresent() && bestCompletion.get().getScore() >= 70)
            .bestScore(bestCompletion.map(LessonCompletion::getScore).orElse(null))
            .attempts((int) attempts)
            .build();
    }

    private void publishLessonStartedEvent(Lesson lesson, String userId) {
        try {
            Unit unit = unitRepository.findById(lesson.getUnitId()).orElse(null);
            Course course = unit != null ? courseRepository.findById(unit.getCourseId()).orElse(null) : null;
            
            eventPublisher.publishLessonStarted(
                userId,
                lesson.getId(),
                lesson.getTitle(),
                lesson.getType().name(),
                course != null ? course.getTargetLanguageCode() : "en",
                lesson.getUnitId(),
                unit != null ? unit.getCourseId() : null
            );
        } catch (Exception e) {
            log.error("Failed to publish lesson.started event", e);
        }
    }

    private void publishLessonCompletedEvent(Lesson lesson, String userId, LessonCompletion completion, boolean isFirstCompletion) {
        try {
            Unit unit = unitRepository.findById(lesson.getUnitId()).orElse(null);
            Course course = unit != null ? courseRepository.findById(unit.getCourseId()).orElse(null) : null;
            
            eventPublisher.publishLessonCompleted(
                userId,
                lesson.getId(),
                lesson.getTitle(),
                lesson.getType().name(),
                course != null ? course.getTargetLanguageCode() : "en",
                lesson.getUnitId(),
                unit != null ? unit.getCourseId() : null,
                completion.getScore(),
                completion.getXpEarned(),
                completion.getCorrectAnswers(),
                completion.getTotalExercises(),
                completion.getTimeSpentSeconds(),
                completion.getAttemptNumber(),
                isFirstCompletion
            );
        } catch (Exception e) {
            log.error("Failed to publish lesson.completed event", e);
        }
    }

    private void checkAndPublishUnitCompleted(Lesson lesson, String userId) {
        try {
            if (unlockService.areAllLessonsCompleted(lesson.getUnitId(), userId)) {
                Unit unit = unitRepository.findById(lesson.getUnitId()).orElse(null);
                if (unit != null) {
                    Course course = courseRepository.findById(unit.getCourseId()).orElse(null);
                    List<Lesson> lessons = lessonRepository.findByUnitIdOrderByOrder(unit.getId());
                    
                    // Calculate stats for the unit
                    List<LessonCompletion> completions = lessonCompletionRepository.findByUserIdAndLessonIdIn(
                        userId, lessons.stream().map(Lesson::getId).collect(Collectors.toList()));
                    
                    int avgScore = (int) completions.stream().mapToInt(LessonCompletion::getScore).average().orElse(0);
                    int totalXP = completions.stream().mapToInt(LessonCompletion::getXpEarned).sum();
                    
                    eventPublisher.publishUnitCompleted(
                        userId,
                        unit.getId(),
                        unit.getTitle(),
                        unit.getCourseId(),
                        course != null ? course.getTargetLanguageCode() : "en",
                        lessons.size(),
                        avgScore,
                        totalXP
                    );
                }
            }
        } catch (Exception e) {
            log.error("Failed to publish unit.completed event", e);
        }
    }

    private void checkAndPublishCourseCompleted(Lesson lesson, String userId) {
        try {
            Unit unit = unitRepository.findById(lesson.getUnitId()).orElse(null);
            if (unit == null) return;
            
            Course course = courseRepository.findById(unit.getCourseId()).orElse(null);
            if (course == null) return;
            
            List<Unit> units = unitRepository.findByCourseIdOrderByOrder(unit.getCourseId());
            boolean allUnitsCompleted = units.stream()
                .allMatch(u -> unlockService.areAllLessonsCompleted(u.getId(), userId));
            
            if (allUnitsCompleted) {
                int totalLessons = units.stream()
                    .mapToInt(u -> (int) lessonRepository.countByUnitId(u.getId()))
                    .sum();
                
                // Get all completions for the course
                List<String> allLessonIds = units.stream()
                    .flatMap(u -> lessonRepository.findByUnitIdOrderByOrder(u.getId()).stream())
                    .map(Lesson::getId)
                    .collect(Collectors.toList());
                
                List<LessonCompletion> completions = lessonCompletionRepository.findByUserIdAndLessonIdIn(userId, allLessonIds);
                int avgScore = (int) completions.stream().mapToInt(LessonCompletion::getScore).average().orElse(0);
                int totalXP = completions.stream().mapToInt(LessonCompletion::getXpEarned).sum();
                
                eventPublisher.publishCourseCompleted(
                    userId,
                    course.getId(),
                    course.getTitle(),
                    course.getLevel(),
                    course.getTargetLanguageCode(),
                    units.size(),
                    totalLessons,
                    avgScore,
                    totalXP
                );
            }
        } catch (Exception e) {
            log.error("Failed to publish course.completed event", e);
        }
    }
}
