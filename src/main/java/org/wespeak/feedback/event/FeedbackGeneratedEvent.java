package org.wespeak.feedback.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackGeneratedEvent {
    private String eventType;
    private String version;
    private Instant timestamp;
    private FeedbackGeneratedPayload payload;
    private EventMetadata metadata;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class FeedbackGeneratedPayload {
    private String feedbackId;
    private String transcriptId;
    private String sessionId;
    private String userId;
    private String targetLanguageCode;
    private Integer overallScore;
    private Integer grammarScore;
    private Integer vocabularyScore;
    private Integer fluencyScore;
    private Integer xpAwarded;
    private Integer errorsCount;
    private String progressTrend;
}
