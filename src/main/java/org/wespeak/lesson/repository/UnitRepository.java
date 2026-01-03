package org.wespeak.lesson.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.wespeak.lesson.entity.Unit;

import java.util.List;

@Repository
public interface UnitRepository extends MongoRepository<Unit, String> {

    /**
     * Find all units for a course, ordered by position.
     */
    List<Unit> findByCourseIdOrderByOrder(String courseId);

    /**
     * Count units in a course.
     */
    long countByCourseId(String courseId);

    /**
     * Find a unit by course and order.
     */
    Unit findByCourseIdAndOrder(String courseId, Integer order);
}
