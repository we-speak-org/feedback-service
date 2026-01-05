package org.wespeak.feedback.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.wespeak.feedback.dto.ProgressDataPoint;
import org.wespeak.feedback.dto.ProgressHistoryResponse;
import org.wespeak.feedback.dto.UserStatsResponse;
import org.wespeak.feedback.entity.Feedback;
import org.wespeak.feedback.entity.UserFeedbackStats;
import org.wespeak.feedback.exception.ResourceNotFoundException;
import org.wespeak.feedback.repository.FeedbackRepository;
import org.wespeak.feedback.repository.UserFeedbackStatsRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

  private final UserFeedbackStatsRepository statsRepository;
  private final FeedbackRepository feedbackRepository;

  public UserStatsResponse getMyStats(String userId, String languageCode) {
    UserFeedbackStats stats =
        statsRepository
            .findByUserIdAndTargetLanguageCode(userId, languageCode)
            .orElseThrow(() -> new ResourceNotFoundException("Stats not found for this language"));

    return UserStatsResponse.builder()
        .userId(stats.getUserId())
        .targetLanguageCode(stats.getTargetLanguageCode())
        .totalSessions(stats.getTotalSessions())
        .totalMinutes(stats.getTotalMinutes())
        .averageOverallScore(stats.getAverageOverallScore())
        .averageGrammarScore(stats.getAverageGrammarScore())
        .averageVocabularyScore(stats.getAverageVocabularyScore())
        .averageFluencyScore(stats.getAverageFluencyScore())
        .commonErrors(stats.getCommonErrors())
        .progressTrend(stats.getProgressTrend())
        .lastFeedbackAt(stats.getLastFeedbackAt())
        .build();
  }

  public ProgressHistoryResponse getMyHistory(String userId, String languageCode, String period) {
    Instant cutoffDate = calculateCutoffDate(period);

    List<Feedback> feedbacks =
        feedbackRepository.findTop5ByUserIdAndTargetLanguageCodeOrderByCreatedAtDesc(
            userId, languageCode);

    // Group by date and calculate average scores
    Map<LocalDate, List<Feedback>> feedbacksByDate =
        feedbacks.stream()
            .filter(f -> f.getCreatedAt().isAfter(cutoffDate))
            .collect(
                Collectors.groupingBy(
                    f -> LocalDate.ofInstant(f.getCreatedAt(), ZoneId.systemDefault())));

    List<ProgressDataPoint> dataPoints =
        feedbacksByDate.entrySet().stream()
            .map(
                entry -> {
                  LocalDate date = entry.getKey();
                  List<Feedback> dayFeedbacks = entry.getValue();

                  int avgScore =
                      (int)
                          dayFeedbacks.stream()
                              .mapToInt(Feedback::getOverallScore)
                              .average()
                              .orElse(0.0);

                  return ProgressDataPoint.builder()
                      .date(date.toString())
                      .overallScore(avgScore)
                      .sessionsCount(dayFeedbacks.size())
                      .build();
                })
            .sorted(Comparator.comparing(ProgressDataPoint::getDate))
            .collect(Collectors.toList());

    return ProgressHistoryResponse.builder()
        .userId(userId)
        .targetLanguageCode(languageCode)
        .period(period)
        .dataPoints(dataPoints)
        .build();
  }

  private Instant calculateCutoffDate(String period) {
    LocalDate now = LocalDate.now();
    return switch (period != null ? period.toUpperCase() : "MONTH") {
      case "WEEK" -> now.minus(7, ChronoUnit.DAYS).atStartOfDay(ZoneId.systemDefault()).toInstant();
      case "MONTH" ->
          now.minus(30, ChronoUnit.DAYS).atStartOfDay(ZoneId.systemDefault()).toInstant();
      case "ALL" -> Instant.EPOCH;
      default -> now.minus(30, ChronoUnit.DAYS).atStartOfDay(ZoneId.systemDefault()).toInstant();
    };
  }
}
