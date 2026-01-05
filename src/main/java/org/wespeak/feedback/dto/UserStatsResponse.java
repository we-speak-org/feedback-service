package org.wespeak.feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.wespeak.feedback.entity.CommonError;
import org.wespeak.feedback.entity.ProgressTrend;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsResponse {
    private String userId;
    private String targetLanguageCode;
    private Integer totalSessions;
    private Integer totalMinutes;
    private Double averageOverallScore;
    private Double averageGrammarScore;
    private Double averageVocabularyScore;
    private Double averageFluencyScore;
    private List<CommonError> commonErrors;
    private ProgressTrend progressTrend;
    private Instant lastFeedbackAt;
}
