package org.wespeak.lesson.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.wespeak.lesson.entity.UserProgress;

import java.util.Optional;

@Repository
public interface UserProgressRepository extends MongoRepository<UserProgress, String> {

    /**
     * Find user progress for a specific language.
     */
    Optional<UserProgress> findByUserIdAndTargetLanguageCode(String userId, String targetLanguageCode);

    /**
     * Check if progress exists for a user and language.
     */
    boolean existsByUserIdAndTargetLanguageCode(String userId, String targetLanguageCode);
}
