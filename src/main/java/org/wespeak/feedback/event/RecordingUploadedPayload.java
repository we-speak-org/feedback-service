package org.wespeak.feedback.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordingUploadedPayload {
  private String recordingId;
  private String sessionId;
  private String participantId;
  private String targetLanguageCode;
  private String audioUrl;
  private Integer duration;
  private String format;
  private Long size;
}
