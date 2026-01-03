package org.wespeak.lesson.listener;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Payload for user.registered event.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredPayload {
    private String userId;
    private String email;
    private String displayName;
    private List<LearningProfile> learningProfiles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearningProfile {
        private String id;
        private String targetLanguageCode;
        private String nativeLanguageCode;
        private String currentLevel;
    }
}
