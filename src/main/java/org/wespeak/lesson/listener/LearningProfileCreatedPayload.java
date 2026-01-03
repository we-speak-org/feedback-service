package org.wespeak.lesson.listener;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for learning_profile.created event.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningProfileCreatedPayload {
    private String userId;
    private String profileId;
    private String targetLanguageCode;
    private String nativeLanguageCode;
    private String currentLevel;
}
