package org.wespeak.feedback.event;

import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XpAwardedEvent {
  private String eventType;
  private String version;
  private Instant timestamp;
  private XpAwardedPayload payload;
  private EventMetadata metadata;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class XpAwardedPayload {
  private String userId;
  private Integer amount;
  private String source;
  private String sourceId;
  private String targetLanguageCode;
  private Map<String, Integer> breakdown;
}
