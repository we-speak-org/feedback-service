package org.wespeak.feedback.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.wespeak.feedback.entity.*;
import org.wespeak.feedback.repository.FeedbackRepository;
import org.wespeak.feedback.repository.UserFeedbackStatsRepository;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

  @Mock private FeedbackRepository feedbackRepository;

  @Mock private UserFeedbackStatsRepository statsRepository;

  @InjectMocks private AnalysisService analysisService;

  private Transcript testTranscript;

  @BeforeEach
  void setUp() {
    testTranscript =
        Transcript.builder()
            .id("trans-123")
            .sessionId("session-456")
            .participantId("user-789")
            .recordingId("rec-001")
            .targetLanguageCode("en")
            .content("Hello, how are you today?")
            .segments(
                List.of(
                    TranscriptSegment.builder()
                        .startTime(0.0)
                        .endTime(2.5)
                        .text("Hello, how are you today?")
                        .confidence(0.95)
                        .build()))
            .duration(120)
            .wordCount(25)
            .confidence(0.93)
            .status(TranscriptStatus.COMPLETED)
            .createdAt(Instant.now())
            .build();
  }

  @Test
  void shouldCreateFeedbackForTranscript() {
    // Given
    Feedback savedFeedback =
        Feedback.builder()
            .id("fb-123")
            .transcriptId(testTranscript.getId())
            .userId(testTranscript.getParticipantId())
            .sessionId(testTranscript.getSessionId())
            .targetLanguageCode(testTranscript.getTargetLanguageCode())
            .status(FeedbackStatus.PENDING)
            .build();

    when(feedbackRepository.save(any(Feedback.class))).thenReturn(savedFeedback);
    when(feedbackRepository.findTop5ByUserIdAndTargetLanguageCodeOrderByCreatedAtDesc(
            anyString(), anyString()))
        .thenReturn(List.of());
    when(statsRepository.findByUserIdAndTargetLanguageCode(anyString(), anyString()))
        .thenReturn(java.util.Optional.empty());
    when(statsRepository.save(any(UserFeedbackStats.class)))
        .thenReturn(UserFeedbackStats.builder().build());

    // When
    analysisService.analyzeTranscript(testTranscript);

    // Then
    verify(feedbackRepository, atLeastOnce()).save(any(Feedback.class));
    verify(statsRepository).save(any(UserFeedbackStats.class));
  }
}
