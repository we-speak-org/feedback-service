package org.wespeak.conversation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.wespeak.conversation.entity.Session;

import java.util.Optional;

@Repository
public interface SessionRepository extends MongoRepository<Session, String> {

    /**
     * Find session by time slot.
     */
    Optional<Session> findByTimeSlotId(String timeSlotId);

    /**
     * Find active session for a time slot.
     */
    Optional<Session> findByTimeSlotIdAndStatusIn(String timeSlotId, Session.Status... statuses);

    /**
     * Find sessions by status.
     */
    Page<Session> findByStatusOrderByCreatedAtDesc(Session.Status status, Pageable pageable);

    /**
     * Find sessions for a language and status.
     */
    Page<Session> findByTargetLanguageCodeAndStatusOrderByCreatedAtDesc(
            String targetLanguageCode, Session.Status status, Pageable pageable);
}
