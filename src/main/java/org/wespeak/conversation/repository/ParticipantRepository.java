package org.wespeak.conversation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.wespeak.conversation.entity.Participant;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantRepository extends MongoRepository<Participant, String> {

    /**
     * Find all participants in a session.
     */
    List<Participant> findBySessionId(String sessionId);

    /**
     * Find active participants in a session.
     */
    List<Participant> findBySessionIdAndStatusNot(String sessionId, Participant.Status status);

    /**
     * Find participant by session and user.
     */
    Optional<Participant> findBySessionIdAndUserId(String sessionId, String userId);

    /**
     * Check if user is in session.
     */
    boolean existsBySessionIdAndUserId(String sessionId, String userId);

    /**
     * Count connected participants in a session.
     */
    long countBySessionIdAndStatus(String sessionId, Participant.Status status);

    /**
     * Count participants who consented to recording.
     */
    long countBySessionIdAndRecordingConsent(String sessionId, Boolean consent);

    /**
     * Find participants who consented to recording.
     */
    List<Participant> findBySessionIdAndRecordingConsent(String sessionId, Boolean consent);

    /**
     * Find session history for a user.
     */
    Page<Participant> findByUserIdOrderByJoinedAtDesc(String userId, Pageable pageable);

    /**
     * Find current active session for a user.
     */
    Optional<Participant> findByUserIdAndStatusNot(String userId, Participant.Status status);
}
