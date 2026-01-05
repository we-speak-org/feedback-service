package org.wespeak.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.wespeak.feedback.dto.FeedbackListResponse;
import org.wespeak.feedback.dto.FeedbackResponse;
import org.wespeak.feedback.entity.Feedback;
import org.wespeak.feedback.entity.FeedbackStatus;
import org.wespeak.feedback.exception.ForbiddenException;
import org.wespeak.feedback.exception.ResourceNotFoundException;
import org.wespeak.feedback.repository.FeedbackRepository;
import org.wespeak.feedback.repository.TranscriptRepository;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

  @Mock private FeedbackRepository feedbackRepository;

  @Mock private TranscriptRepository transcriptRepository;

  @InjectMocks private FeedbackService feedbackService;

  private Feedback testFeedback;

  @BeforeEach
  void setUp() {
    testFeedback =
        Feedback.builder()
            .id("fb-123")
            .transcriptId("trans-123")
            .userId("user-789")
            .sessionId("session-456")
            .targetLanguageCode("en")
            .overallScore(72)
            .grammarScore(68)
            .vocabularyScore(75)
            .fluencyScore(78)
            .xpAwarded(25)
            .status(FeedbackStatus.COMPLETED)
            .createdAt(Instant.now())
            .build();
  }

  @Test
  void shouldReturnFeedbackWhenUserIsOwner() {
    // Given
    when(feedbackRepository.findById("fb-123")).thenReturn(Optional.of(testFeedback));

    // When
    FeedbackResponse response = feedbackService.getFeedback("fb-123", "user-789");

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo("fb-123");
    assertThat(response.getOverallScore()).isEqualTo(72);
  }

  @Test
  void shouldThrowForbiddenWhenUserIsNotOwner() {
    // Given
    when(feedbackRepository.findById("fb-123")).thenReturn(Optional.of(testFeedback));

    // When/Then
    assertThatThrownBy(() -> feedbackService.getFeedback("fb-123", "other-user"))
        .isInstanceOf(ForbiddenException.class)
        .hasMessageContaining("Access denied");
  }

  @Test
  void shouldThrowNotFoundWhenFeedbackDoesNotExist() {
    // Given
    when(feedbackRepository.findById("fb-999")).thenReturn(Optional.empty());

    // When/Then
    assertThatThrownBy(() -> feedbackService.getFeedback("fb-999", "user-789"))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("not found");
  }

  @Test
  void shouldReturnPaginatedFeedbacks() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);
    Page<Feedback> feedbackPage = new PageImpl<>(List.of(testFeedback), pageable, 1);

    when(feedbackRepository.findByUserIdAndTargetLanguageCode(
            eq("user-789"), eq("en"), any(Pageable.class)))
        .thenReturn(feedbackPage);

    // When
    FeedbackListResponse response = feedbackService.getMyFeedbacks("user-789", "en", pageable);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getItems()).hasSize(1);
    assertThat(response.getTotal()).isEqualTo(1L);
    assertThat(response.getPage()).isEqualTo(0);
  }
}
