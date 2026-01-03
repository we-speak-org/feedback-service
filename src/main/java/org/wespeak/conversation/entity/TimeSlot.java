package org.wespeak.conversation.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * TimeSlot entity - Represents a scheduled time slot for conversation sessions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "timeslots")
@CompoundIndex(name = "idx_timeslot_lang_level_time", def = "{'targetLanguageCode': 1, 'level': 1, 'startTime': 1}")
@CompoundIndex(name = "idx_timeslot_time_active", def = "{'startTime': 1, 'isActive': 1}")
public class TimeSlot {

    @Id
    private String id;

    /** Target language code (e.g., "en", "fr", "es") */
    private String targetLanguageCode;

    /** CEFR level: A1, A2, B1, B2, C1, C2 */
    private Level level;

    /** Start time of the slot */
    private Instant startTime;

    /** Duration in minutes: 15, 30, or 45 */
    private Integer durationMinutes;

    /** Maximum number of participants (default: 8) */
    @Builder.Default
    private Integer maxParticipants = 8;

    /** Minimum participants to start (default: 2) */
    @Builder.Default
    private Integer minParticipants = 2;

    /** Recurrence pattern */
    private Recurrence recurrence;

    /** Whether the slot is available */
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum Level {
        A1, A2, B1, B2, C1, C2
    }

    public enum Recurrence {
        once, daily, weekly
    }

    public Instant getEndTime() {
        return startTime.plusSeconds(durationMinutes * 60L);
    }
}
