package org.wespeak.conversation.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Registration entity - User registration for a time slot.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "registrations")
@CompoundIndex(name = "idx_reg_timeslot_status", def = "{'timeSlotId': 1, 'status': 1}")
@CompoundIndex(name = "idx_reg_user_status", def = "{'userId': 1, 'status': 1}")
@CompoundIndex(name = "idx_reg_timeslot_user", def = "{'timeSlotId': 1, 'userId': 1}", unique = true)
public class Registration {

    @Id
    private String id;

    /** Reference to the time slot */
    private String timeSlotId;

    /** User ID */
    private String userId;

    /** Registration status */
    @Builder.Default
    private Status status = Status.registered;

    /** Registration timestamp */
    @CreatedDate
    private Instant registeredAt;

    /** Cancellation timestamp */
    private Instant cancelledAt;

    public enum Status {
        registered,  // User is registered
        cancelled,   // User cancelled
        attended,    // User participated
        noshow       // User didn't show up
    }
}
