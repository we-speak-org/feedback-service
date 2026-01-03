package org.wespeak.conversation.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.wespeak.conversation.entity.Registration;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends MongoRepository<Registration, String> {

    /**
     * Find registration for a user and time slot.
     */
    Optional<Registration> findByTimeSlotIdAndUserId(String timeSlotId, String userId);

    /**
     * Find all active registrations for a time slot.
     */
    List<Registration> findByTimeSlotIdAndStatus(String timeSlotId, Registration.Status status);

    /**
     * Count active registrations for a time slot.
     */
    long countByTimeSlotIdAndStatus(String timeSlotId, Registration.Status status);

    /**
     * Find all active registrations for a user.
     */
    List<Registration> findByUserIdAndStatus(String userId, Registration.Status status);

    /**
     * Count active registrations for a user.
     */
    long countByUserIdAndStatus(String userId, Registration.Status status);

    /**
     * Check if user is registered for a time slot.
     */
    boolean existsByTimeSlotIdAndUserIdAndStatus(String timeSlotId, String userId, Registration.Status status);

    /**
     * Find all registrations for a user (any status).
     */
    List<Registration> findByUserIdOrderByRegisteredAtDesc(String userId);
}
