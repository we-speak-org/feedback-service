package org.wespeak.feedback.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressHistoryResponse {
  private String userId;
  private String targetLanguageCode;
  private String period;
  private List<ProgressDataPoint> dataPoints;
}
