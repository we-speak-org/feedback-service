package org.wespeak.conversation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.wespeak.conversation.entity.TimeSlot;

import java.time.Instant;
import java.util.List;

@Repository
public interface TimeSlotRepository extends MongoRepository<TimeSlot, String> {

    /**
     * Find active time slots for a language and level in a date range.
     */
    List<TimeSlot> findByTargetLanguageCodeAndLevelAndIsActiveAndStartTimeBetweenOrderByStartTime(
            String targetLanguageCode, TimeSlot.Level level, Boolean isActive, Instant from, Instant to);

    /**
     * Find active time slots for a language in a date range.
     */
    List<TimeSlot> findByTargetLanguageCodeAndIsActiveAndStartTimeBetweenOrderByStartTime(
            String targetLanguageCode, Boolean isActive, Instant from, Instant to);

    /**
     * Find active time slots in a date range (no filter).
     */
    Page<TimeSlot> findByIsActiveAndStartTimeBetweenOrderByStartTime(
            Boolean isActive, Instant from, Instant to, Pageable pageable);

    /**
     * Find time slots that need to have sessions created.
     */
    List<TimeSlot> findByStartTimeBetweenAndIsActive(Instant from, Instant to, Boolean isActive);

    /**
     * Check if a time slot exists at a given time for a language/level.
     */
    boolean existsByTargetLanguageCodeAndLevelAndStartTime(
            String targetLanguageCode, TimeSlot.Level level, Instant startTime);
}
