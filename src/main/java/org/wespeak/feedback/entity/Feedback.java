package org.wespeak.feedback.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "feedbacks")
@CompoundIndex(name = "user_language_idx", def = "{'userId': 1, 'targetLanguageCode': 1}")
public class Feedback {
    @Id
    @Builder.Default
    private String id = UUID.randomUUID().toString();
    
    @Indexed
    private String transcriptId;
    
    @Indexed
    private String userId;
    
    @Indexed
    private String sessionId;
    
    @Indexed
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
    
    @Builder.Default
    private FeedbackStatus status = FeedbackStatus.PENDING;
    
    @CreatedDate
    private Instant createdAt;
    
    private Instant completedAt;
}
