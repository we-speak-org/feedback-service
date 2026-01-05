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
public class FeedbackListResponse {
    private List<FeedbackListItem> items;
    private Integer page;
    private Integer size;
    private Long total;
    private Integer totalPages;
}
