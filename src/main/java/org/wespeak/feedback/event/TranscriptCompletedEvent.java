package org.wespeak.feedback.event;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptCompletedEvent {
  private String eventType;
  private String version;
  private Instant timestamp;
  private TranscriptCompletedPayload payload;
  private EventMetadata metadata;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class TranscriptCompletedPayload {
  private String transcriptId;
  private String sessionId;
  private String participantId;
  private String targetLanguageCode;
  private Integer wordCount;
  private Integer duration;
  private Double confidence;
}
