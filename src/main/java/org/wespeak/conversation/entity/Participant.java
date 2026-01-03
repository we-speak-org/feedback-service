package org.wespeak.conversation.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Participant entity - Represents a user in a session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "participants")
@CompoundIndex(name = "idx_participant_user_status", def = "{'userId': 1, 'status': 1}")
public class Participant {

    @Id
    private String id;

    /** Reference to the session */
    @Indexed
    private String sessionId;

    /** User ID */
    private String userId;

    /** Display name */
    private String displayName;

    /** Connection status */
    @Builder.Default
    private Status status = Status.waiting;

    /** Camera state */
    @Builder.Default
    private Boolean cameraEnabled = true;

    /** Microphone state */
    @Builder.Default
    private Boolean micEnabled = true;

    /** Recording consent */
    @Builder.Default
    private Boolean recordingConsent = false;

    /** Time when participant joined */
    private Instant joinedAt;

    /** Time when participant left */
    private Instant leftAt;

    public enum Status {
        waiting,      // Connected to session, waiting for start
        connected,    // Actively participating
        disconnected  // Left or disconnected
    }
}
