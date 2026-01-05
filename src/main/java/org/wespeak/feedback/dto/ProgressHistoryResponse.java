package org.wespeak.feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
