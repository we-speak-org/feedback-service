package org.wespeak.lesson.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.wespeak.lesson.dto.*;
import org.wespeak.lesson.entity.*;
import org.wespeak.lesson.exception.ResourceNotFoundException;
import org.wespeak.lesson.repository.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final UserProgressRepository userProgressRepository;
    private final LessonCompletionRepository lessonCompletionRepository;
    private final CourseRepository courseRepository;
    private final UnitRepository unitRepository;
    private final LessonRepository lessonRepository;
    private final UnlockService unlockService;

    /**
     * Get user progress for a language.
     */
    public UserProgressDto getProgress(String userId, String languageCode) {
        UserProgress progress = userProgressRepository.findByUserIdAndTargetLanguageCode(userId, languageCode)
            .orElseThrow(() -> ResourceNotFoundException.progressNotFound(userId, languageCode));

        // Get current course, unit, lesson details
        UserProgressDto.CourseRefDto courseRef = null;
        UnitRefDto unitRef = null;
        UserProgressDto.LessonRefDto lessonRef = null;

        if (progress.getCurrentCourseId() != null) {
            Course course = courseRepository.findById(progress.getCurrentCourseId()).orElse(null);
            if (course != null) {
                courseRef = UserProgressDto.CourseRefDto.builder()
                    .id(course.getId())
                    .title(course.getTitle())
                    .level(course.getLevel())
                    .build();
            }
        }

        if (progress.getCurrentUnitId() != null) {
            Unit unit = unitRepository.findById(progress.getCurrentUnitId()).orElse(null);
            if (unit != null) {
                unitRef = UnitRefDto.builder()
                    .id(unit.getId())
                    .title(unit.getTitle())
                    .build();
            }
        }

        if (progress.getCurrentLessonId() != null) {
            Lesson lesson = lessonRepository.findById(progress.getCurrentLessonId()).orElse(null);
            if (lesson != null) {
                lessonRef = UserProgressDto.LessonRefDto.builder()
                    .id(lesson.getId())
                    .title(lesson.getTitle())
                    .build();
            }
        }

        return UserProgressDto.builder()
            .userId(userId)
            .targetLanguageCode(languageCode)
            .currentCourse(courseRef)
            .currentUnit(unitRef)
            .currentLesson(lessonRef)
            .stats(UserProgressDto.StatsDto.builder()
                .lessonsCompleted(progress.getLessonsCompleted())
                .averageScore(progress.getAverageScore())
                .totalTimeMinutes(progress.getTotalTimeMinutes())
                .build())
            .lastActivityAt(progress.getLastActivityAt())
            .build();
    }

    /**
     * Get completion history for a user.
     */
    public ProgressHistoryDto getHistory(String userId, String languageCode, int limit, int offset) {
        Pageable pageable = PageRequest.of(offset / limit, limit);
        Page<LessonCompletion> completionsPage = lessonCompletionRepository
            .findByUserIdOrderByCompletedAtDesc(userId, pageable);

        // Filter by language and build DTOs
        List<ProgressHistoryDto.CompletionHistoryItem> items = completionsPage.getContent().stream()
            .map(completion -> {
                Lesson lesson = lessonRepository.findById(completion.getLessonId()).orElse(null);
                return ProgressHistoryDto.CompletionHistoryItem.builder()
                    .lessonId(completion.getLessonId())
                    .lessonTitle(lesson != null ? lesson.getTitle() : "Unknown")
                    .score(completion.getScore())
                    .xpEarned(completion.getXpEarned())
                    .completedAt(completion.getCompletedAt())
                    .build();
            })
            .collect(Collectors.toList());

        return ProgressHistoryDto.builder()
            .completions(items)
            .total(completionsPage.getTotalElements())
            .hasMore(completionsPage.hasNext())
            .build();
    }

    /**
     * Get unlocked lessons for a user.
     */
    public UnlockedLessonsDto getUnlockedLessons(String userId, String languageCode) {
        // Get all courses for the language
        List<Course> courses = courseRepository.findByTargetLanguageCodeAndIsPublishedOrderByOrder(languageCode, true);
        
        List<UnlockedLessonsDto.UnlockedLessonItem> unlockedItems = courses.stream()
            .flatMap(course -> unitRepository.findByCourseIdOrderByOrder(course.getId()).stream())
            .flatMap(unit -> lessonRepository.findByUnitIdOrderByOrder(unit.getId()).stream())
            .filter(lesson -> unlockService.isLessonUnlocked(lesson.getId(), userId))
            .map(lesson -> {
                Optional<LessonCompletion> best = lessonCompletionRepository
                    .findFirstByUserIdAndLessonIdOrderByScoreDesc(userId, lesson.getId());
                return UnlockedLessonsDto.UnlockedLessonItem.builder()
                    .lessonId(lesson.getId())
                    .lessonTitle(lesson.getTitle())
                    .isCompleted(best.isPresent() && best.get().getScore() >= 70)
                    .bestScore(best.map(LessonCompletion::getScore).orElse(null))
                    .build();
            })
            .collect(Collectors.toList());

        // Find next to unlock
        UnlockedLessonsDto.NextToUnlockDto nextToUnlock = findNextToUnlock(userId, languageCode, courses);

        return UnlockedLessonsDto.builder()
            .unlockedLessons(unlockedItems)
            .nextToUnlock(nextToUnlock)
            .build();
    }

    /**
     * Initialize progress for a user when they start learning a language.
     * Called when consuming user.registered or learning_profile.created events.
     */
    public void initializeProgress(String userId, String languageCode) {
        if (userProgressRepository.existsByUserIdAndTargetLanguageCode(userId, languageCode)) {
            log.info("Progress already exists for user {} and language {}", userId, languageCode);
            return;
        }

        // Find the first course for this language
        List<Course> courses = courseRepository.findByTargetLanguageCodeAndIsPublishedOrderByOrder(languageCode, true);
        String firstCourseId = null;
        String firstUnitId = null;
        String firstLessonId = null;

        if (!courses.isEmpty()) {
            Course firstCourse = courses.get(0);
            firstCourseId = firstCourse.getId();
            
            List<Unit> units = unitRepository.findByCourseIdOrderByOrder(firstCourseId);
            if (!units.isEmpty()) {
                firstUnitId = units.get(0).getId();
                
                List<Lesson> lessons = lessonRepository.findByUnitIdOrderByOrder(firstUnitId);
                if (!lessons.isEmpty()) {
                    firstLessonId = lessons.get(0).getId();
                }
            }
        }

        UserProgress progress = UserProgress.builder()
            .userId(userId)
            .targetLanguageCode(languageCode)
            .currentCourseId(firstCourseId)
            .currentUnitId(firstUnitId)
            .currentLessonId(firstLessonId)
            .lessonsCompleted(0)
            .averageScore(0)
            .totalTimeMinutes(0)
            .lastActivityAt(Instant.now())
            .build();

        userProgressRepository.save(progress);
        log.info("Created progress for user {} learning {}", userId, languageCode);
    }

    private UnlockedLessonsDto.NextToUnlockDto findNextToUnlock(String userId, String languageCode, List<Course> courses) {
        for (Course course : courses) {
            List<Unit> units = unitRepository.findByCourseIdOrderByOrder(course.getId());
            for (Unit unit : units) {
                List<Lesson> lessons = lessonRepository.findByUnitIdOrderByOrder(unit.getId());
                for (Lesson lesson : lessons) {
                    if (!unlockService.isLessonUnlocked(lesson.getId(), userId)) {
                        // This is the next locked lesson
                        Optional<Lesson> previousLesson = unlockService.getPreviousLesson(lesson.getId());
                        String requirement = previousLesson
                            .map(prev -> String.format("Complete '%s' with 70%%+", prev.getTitle()))
                            .orElse("Complete previous lessons");
                        
                        return UnlockedLessonsDto.NextToUnlockDto.builder()
                            .lessonId(lesson.getId())
                            .lessonTitle(lesson.getTitle())
                            .requirement(requirement)
                            .build();
                    }
                }
            }
        }
        return null;
    }
}
