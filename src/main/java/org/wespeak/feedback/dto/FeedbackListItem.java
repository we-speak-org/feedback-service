package org.wespeak.feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackListItem {
    private String id;
    private String sessionId;
    private String targetLanguageCode;
    private Integer overallScore;
    private Integer xpAwarded;
    private Instant createdAt;
}
