package org.wespeak.lesson.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.wespeak.lesson.entity.LessonCompletion;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonCompletionRepository extends MongoRepository<LessonCompletion, String> {

    /**
     * Find completions for a user and lesson, ordered by date descending.
     */
    List<LessonCompletion> findByUserIdAndLessonIdOrderByCompletedAtDesc(String userId, String lessonId);

    /**
     * Find the best completion (highest score) for a user and lesson.
     */
    Optional<LessonCompletion> findFirstByUserIdAndLessonIdOrderByScoreDesc(String userId, String lessonId);

    /**
     * Find all completions for a user, paginated.
     */
    Page<LessonCompletion> findByUserIdOrderByCompletedAtDesc(String userId, Pageable pageable);

    /**
     * Count attempts for a user and lesson.
     */
    long countByUserIdAndLessonId(String userId, String lessonId);

    /**
     * Check if user has completed a lesson with minimum score.
     */
    boolean existsByUserIdAndLessonIdAndScoreGreaterThanEqual(String userId, String lessonId, Integer minScore);

    /**
     * Find all completions for lessons in a list.
     */
    List<LessonCompletion> findByUserIdAndLessonIdIn(String userId, List<String> lessonIds);
}
