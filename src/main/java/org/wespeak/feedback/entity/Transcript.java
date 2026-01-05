package org.wespeak.feedback.entity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transcripts")
@CompoundIndex(name = "session_participant_idx", def = "{'sessionId': 1, 'participantId': 1}")
public class Transcript {
  @Id @Builder.Default private String id = UUID.randomUUID().toString();

  @Indexed private String sessionId;

  @Indexed private String participantId;

  private String recordingId;

  @Indexed private String targetLanguageCode;

  private String content;

  private List<TranscriptSegment> segments;

  private Integer duration;

  private Integer wordCount;

  private Double confidence;

  @Builder.Default private TranscriptStatus status = TranscriptStatus.PENDING;

  @CreatedDate private Instant createdAt;

  private Instant completedAt;
}
