package org.wespeak.feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.wespeak.feedback.entity.FeedbackError;
import org.wespeak.feedback.entity.FeedbackStatus;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {
    private String id;
    private String transcriptId;
    private String userId;
    private String sessionId;
    private String targetLanguageCode;
    private Integer overallScore;
    private Integer grammarScore;
    private Integer vocabularyScore;
    private Integer fluencyScore;
    private Integer pronunciationScore;
    private List<FeedbackError> errors;
    private List<String> strengths;
    private List<String> improvements;
    private String summary;
    private Integer xpAwarded;
    private FeedbackStatus status;
    private Instant createdAt;
    private Instant completedAt;
}
