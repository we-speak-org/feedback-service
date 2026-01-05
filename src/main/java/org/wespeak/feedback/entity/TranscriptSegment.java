package org.wespeak.feedback.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptSegment {
    private Double startTime;
    private Double endTime;
    private String text;
    private Double confidence;
}
