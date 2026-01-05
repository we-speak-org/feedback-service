package org.wespeak.feedback.entity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_feedback_stats")
@CompoundIndex(
    name = "user_language_unique_idx",
    def = "{'userId': 1, 'targetLanguageCode': 1}",
    unique = true)
public class UserFeedbackStats {
  @Id @Builder.Default private String id = UUID.randomUUID().toString();

  private String userId;

  private String targetLanguageCode;

  @Builder.Default private Integer totalSessions = 0;

  @Builder.Default private Integer totalMinutes = 0;

  @Builder.Default private Double averageOverallScore = 0.0;

  @Builder.Default private Double averageGrammarScore = 0.0;

  @Builder.Default private Double averageVocabularyScore = 0.0;

  @Builder.Default private Double averageFluencyScore = 0.0;

  private List<CommonError> commonErrors;

  @Builder.Default private ProgressTrend progressTrend = ProgressTrend.STABLE;

  private Instant lastFeedbackAt;

  @LastModifiedDate private Instant updatedAt;
}
