package org.wespeak.lesson.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.wespeak.lesson.entity.Course;

import java.util.List;

@Repository
public interface CourseRepository extends MongoRepository<Course, String> {

    /**
     * Find all published courses for a language.
     */
    List<Course> findByTargetLanguageCodeAndIsPublishedOrderByOrder(String targetLanguageCode, Boolean isPublished);

    /**
     * Find all published courses for a language and level.
     */
    List<Course> findByTargetLanguageCodeAndLevelAndIsPublishedOrderByOrder(
            String targetLanguageCode, String level, Boolean isPublished);

    /**
     * Find all courses for a language (including unpublished).
     */
    List<Course> findByTargetLanguageCodeOrderByOrder(String targetLanguageCode);

    /**
     * Count courses by language.
     */
    long countByTargetLanguageCodeAndIsPublished(String targetLanguageCode, Boolean isPublished);
}
