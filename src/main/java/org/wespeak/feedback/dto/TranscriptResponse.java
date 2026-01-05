package org.wespeak.feedback.dto;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.wespeak.feedback.entity.TranscriptSegment;
import org.wespeak.feedback.entity.TranscriptStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptResponse {
  private String id;
  private String sessionId;
  private String participantId;
  private String targetLanguageCode;
  private String content;
  private List<TranscriptSegment> segments;
  private Integer duration;
  private Integer wordCount;
  private Double confidence;
  private TranscriptStatus status;
  private Instant createdAt;
  private Instant completedAt;
}
