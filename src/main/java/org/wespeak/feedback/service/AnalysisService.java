package org.wespeak.feedback.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.wespeak.feedback.entity.*;
import org.wespeak.feedback.repository.FeedbackRepository;
import org.wespeak.feedback.repository.UserFeedbackStatsRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {
    
    private final FeedbackRepository feedbackRepository;
    private final UserFeedbackStatsRepository statsRepository;
    
    public void analyzeTranscript(Transcript transcript) {
        log.info("Starting analysis for transcript: {}", transcript.getId());
        
        // Create Feedback in PENDING status
        Feedback feedback = Feedback.builder()
                .transcriptId(transcript.getId())
                .userId(transcript.getParticipantId())
                .sessionId(transcript.getSessionId())
                .targetLanguageCode(transcript.getTargetLanguageCode())
                .status(FeedbackStatus.PENDING)
                .build();
        
        feedback = feedbackRepository.save(feedback);
        
        try {
            feedback.setStatus(FeedbackStatus.PROCESSING);
            feedbackRepository.save(feedback);
            
            // STUBBED: In production, this would call LLM API (Claude/GPT)
            // For now, create a mock analysis
            
            // Mock errors found in the transcript
            List<FeedbackError> mockErrors = List.of(
                    FeedbackError.builder()
                            .type(ErrorType.GRAMMAR)
                            .original("I go yesterday")
                            .correction("I went yesterday")
                            .explanation("Utilisez le passé simple 'went' pour une action passée.")
                            .severity(ErrorSeverity.MEDIUM)
                            .segmentIndex(2)
                            .build(),
                    FeedbackError.builder()
                            .type(ErrorType.VOCABULARY)
                            .original("many peoples")
                            .correction("many people")
                            .explanation("'People' est déjà un pluriel. N'ajoutez pas 's'.")
                            .severity(ErrorSeverity.LOW)
                            .segmentIndex(2)
                            .build()
            );
            
            List<String> mockStrengths = List.of(
                    "Bonne fluidité générale dans l'expression",
                    "Utilisation correcte des temps présents",
                    "Vocabulaire approprié pour décrire des activités quotidiennes"
            );
            
            List<String> mockImprovements = List.of(
                    "Revoir la conjugaison des verbes irréguliers au passé",
                    "Attention aux pluriels irréguliers en anglais",
                    "Pratiquer l'utilisation des articles (a, an, the)"
            );
            
            String mockSummary = "Bon travail ! Votre anglais est compréhensible et fluide. " +
                    "Quelques erreurs de grammaire à corriger, notamment sur les temps du passé. " +
                    "Continuez à pratiquer régulièrement.";
            
            // Calculate scores
            int grammarScore = 68;
            int vocabularyScore = 75;
            int fluencyScore = 78;
            int pronunciationScore = 70;
            
            // Calculate overall score (weighted average)
            int overallScore = (int) ((grammarScore * 0.35) + (vocabularyScore * 0.25) + 
                                      (fluencyScore * 0.25) + (pronunciationScore * 0.15));
            
            // Calculate XP
            int xpAwarded = calculateXp(overallScore, transcript.getDuration(), transcript.getParticipantId(), 
                                       transcript.getTargetLanguageCode());
            
            // Update feedback
            feedback.setOverallScore(overallScore);
            feedback.setGrammarScore(grammarScore);
            feedback.setVocabularyScore(vocabularyScore);
            feedback.setFluencyScore(fluencyScore);
            feedback.setPronunciationScore(pronunciationScore);
            feedback.setErrors(mockErrors);
            feedback.setStrengths(mockStrengths);
            feedback.setImprovements(mockImprovements);
            feedback.setSummary(mockSummary);
            feedback.setXpAwarded(xpAwarded);
            feedback.setStatus(FeedbackStatus.COMPLETED);
            feedback.setCompletedAt(Instant.now());
            
            feedback = feedbackRepository.save(feedback);
            log.info("Analysis completed for transcript: {}", transcript.getId());
            
            // Update user stats
            updateUserStats(feedback, transcript.getDuration());
            
            // STUBBED: Publish Kafka events (feedback.generated, xp.awarded)
            log.info("Would publish Kafka events: feedback.generated and xp.awarded");
            
        } catch (Exception e) {
            log.error("Analysis failed for transcript: {}", transcript.getId(), e);
            feedback.setStatus(FeedbackStatus.FAILED);
            feedbackRepository.save(feedback);
        }
    }
    
    private int calculateXp(int overallScore, int durationSeconds, String userId, String languageCode) {
        int xp = 10; // Base participation XP
        
        if (overallScore >= 60) {
            xp += 5;
        }
        if (overallScore >= 80) {
            xp += 10;
        }
        
        // Duration bonuses
        if (durationSeconds >= 600) { // 10 minutes
            xp += 5;
        }
        if (durationSeconds >= 1200) { // 20 minutes
            xp += 10;
        }
        
        // Check if improving (compare with previous sessions)
        if (isImproving(userId, languageCode, overallScore)) {
            xp += 5;
        }
        
        return Math.min(xp, 40); // Max 40 XP
    }
    
    private boolean isImproving(String userId, String languageCode, int currentScore) {
        List<Feedback> recentFeedbacks = feedbackRepository
                .findTop5ByUserIdAndTargetLanguageCodeOrderByCreatedAtDesc(userId, languageCode);
        
        if (recentFeedbacks.size() < 2) {
            return false;
        }
        
        // Compare with previous session
        Feedback previousFeedback = recentFeedbacks.get(0);
        return currentScore > previousFeedback.getOverallScore();
    }
    
    private void updateUserStats(Feedback feedback, int durationSeconds) {
        UserFeedbackStats stats = statsRepository
                .findByUserIdAndTargetLanguageCode(feedback.getUserId(), feedback.getTargetLanguageCode())
                .orElse(UserFeedbackStats.builder()
                        .userId(feedback.getUserId())
                        .targetLanguageCode(feedback.getTargetLanguageCode())
                        .commonErrors(new ArrayList<>())
                        .build());
        
        // Update totals
        stats.setTotalSessions(stats.getTotalSessions() + 1);
        stats.setTotalMinutes(stats.getTotalMinutes() + (durationSeconds / 60));
        
        // Recalculate averages
        int totalSessions = stats.getTotalSessions();
        stats.setAverageOverallScore(
                ((stats.getAverageOverallScore() * (totalSessions - 1)) + feedback.getOverallScore()) / totalSessions);
        stats.setAverageGrammarScore(
                ((stats.getAverageGrammarScore() * (totalSessions - 1)) + feedback.getGrammarScore()) / totalSessions);
        stats.setAverageVocabularyScore(
                ((stats.getAverageVocabularyScore() * (totalSessions - 1)) + feedback.getVocabularyScore()) / totalSessions);
        stats.setAverageFluencyScore(
                ((stats.getAverageFluencyScore() * (totalSessions - 1)) + feedback.getFluencyScore()) / totalSessions);
        
        // Update progress trend
        stats.setProgressTrend(calculateProgressTrend(feedback.getUserId(), feedback.getTargetLanguageCode()));
        
        stats.setLastFeedbackAt(Instant.now());
        
        statsRepository.save(stats);
        log.info("Updated stats for user: {}, language: {}", feedback.getUserId(), feedback.getTargetLanguageCode());
    }
    
    private ProgressTrend calculateProgressTrend(String userId, String languageCode) {
        List<Feedback> recentFeedbacks = feedbackRepository
                .findTop5ByUserIdAndTargetLanguageCodeOrderByCreatedAtDesc(userId, languageCode);
        
        if (recentFeedbacks.size() < 3) {
            return ProgressTrend.STABLE;
        }
        
        double recentAvg = recentFeedbacks.stream()
                .limit(2)
                .mapToInt(Feedback::getOverallScore)
                .average()
                .orElse(0.0);
        
        double olderAvg = recentFeedbacks.stream()
                .skip(2)
                .mapToInt(Feedback::getOverallScore)
                .average()
                .orElse(0.0);
        
        double diff = recentAvg - olderAvg;
        
        if (diff >= 5) {
            return ProgressTrend.IMPROVING;
        } else if (diff <= -5) {
            return ProgressTrend.DECLINING;
        } else {
            return ProgressTrend.STABLE;
        }
    }
}
