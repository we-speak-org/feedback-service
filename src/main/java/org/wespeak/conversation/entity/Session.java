package org.wespeak.conversation.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Session entity - Represents a conversation session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "sessions")
@CompoundIndex(name = "idx_session_status_lang", def = "{'status': 1, 'targetLanguageCode': 1}")
public class Session {

    @Id
    private String id;

    /** Reference to the time slot */
    @Indexed
    private String timeSlotId;

    /** Target language being practiced */
    private String targetLanguageCode;

    /** CEFR level */
    private TimeSlot.Level level;

    /** Session status */
    @Builder.Default
    private Status status = Status.waiting;

    /** Actual start time */
    private Instant startedAt;

    /** End time */
    private Instant endedAt;

    /** Whether recording is enabled (at least 1 participant consented) */
    @Builder.Default
    private Boolean recordingEnabled = false;

    /** S3 URL of the recording */
    private String recordingUrl;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum Status {
        waiting,  // Waiting for participants
        active,   // Session in progress
        ended     // Session completed
    }
}
