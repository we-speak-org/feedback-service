package org.wespeak.lesson.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.wespeak.lesson.entity.Lesson;

import java.util.List;

@Repository
public interface LessonRepository extends MongoRepository<Lesson, String> {

    /**
     * Find all lessons for a unit, ordered by position.
     */
    List<Lesson> findByUnitIdOrderByOrder(String unitId);

    /**
     * Count lessons in a unit.
     */
    long countByUnitId(String unitId);

    /**
     * Find a lesson by unit and order.
     */
    Lesson findByUnitIdAndOrder(String unitId, Integer order);

    /**
     * Find all lessons for multiple units.
     */
    List<Lesson> findByUnitIdIn(List<String> unitIds);
}
