package org.wespeak.feedback.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackError {
  private ErrorType type;
  private String original;
  private String correction;
  private String explanation;
  private ErrorSeverity severity;
  private Integer segmentIndex;
}
