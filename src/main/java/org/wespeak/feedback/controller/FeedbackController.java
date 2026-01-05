package org.wespeak.feedback.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.wespeak.feedback.dto.*;
import org.wespeak.feedback.service.FeedbackService;
import org.wespeak.feedback.service.StatsService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedbackController {
    
    private final FeedbackService feedbackService;
    private final StatsService statsService;
    
    // Transcripts endpoints
    
    @GetMapping("/transcripts/{transcriptId}")
    public ResponseEntity<TranscriptResponse> getTranscript(
            @PathVariable String transcriptId,
            @RequestHeader(value = "X-User-Id", defaultValue = "test-user") String userId) {
        log.info("Getting transcript: {} for user: {}", transcriptId, userId);
        return ResponseEntity.ok(feedbackService.getTranscript(transcriptId, userId));
    }
    
    @GetMapping("/transcripts")
    public ResponseEntity<List<TranscriptResponse>> getTranscriptsBySession(
            @RequestParam String sessionId,
            @RequestHeader(value = "X-User-Id", defaultValue = "test-user") String userId) {
        log.info("Getting transcripts for session: {} and user: {}", sessionId, userId);
        return ResponseEntity.ok(feedbackService.getTranscriptsBySession(sessionId, userId));
    }
    
    // Feedbacks endpoints
    
    @GetMapping("/feedbacks/{feedbackId}")
    public ResponseEntity<FeedbackResponse> getFeedback(
            @PathVariable String feedbackId,
            @RequestHeader(value = "X-User-Id", defaultValue = "test-user") String userId) {
        log.info("Getting feedback: {} for user: {}", feedbackId, userId);
        return ResponseEntity.ok(feedbackService.getFeedback(feedbackId, userId));
    }
    
    @GetMapping("/feedbacks/me")
    public ResponseEntity<FeedbackListResponse> getMyFeedbacks(
            @RequestParam(required = false) String targetLanguageCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "X-User-Id", defaultValue = "test-user") String userId) {
        log.info("Getting feedbacks for user: {}, language: {}, page: {}, size: {}", 
                userId, targetLanguageCode, page, size);
        
        // Limit max size
        if (size > 50) {
            size = 50;
        }
        
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(feedbackService.getMyFeedbacks(userId, targetLanguageCode, pageable));
    }
    
    @GetMapping("/feedbacks/session/{sessionId}")
    public ResponseEntity<FeedbackResponse> getFeedbackBySession(
            @PathVariable String sessionId,
            @RequestHeader(value = "X-User-Id", defaultValue = "test-user") String userId) {
        log.info("Getting feedback for session: {} and user: {}", sessionId, userId);
        return ResponseEntity.ok(feedbackService.getFeedbackBySession(sessionId, userId));
    }
    
    // Stats endpoints
    
    @GetMapping("/stats/me")
    public ResponseEntity<UserStatsResponse> getMyStats(
            @RequestParam String targetLanguageCode,
            @RequestHeader(value = "X-User-Id", defaultValue = "test-user") String userId) {
        log.info("Getting stats for user: {} and language: {}", userId, targetLanguageCode);
        return ResponseEntity.ok(statsService.getMyStats(userId, targetLanguageCode));
    }
    
    @GetMapping("/stats/me/history")
    public ResponseEntity<ProgressHistoryResponse> getMyHistory(
            @RequestParam String targetLanguageCode,
            @RequestParam(defaultValue = "MONTH") String period,
            @RequestHeader(value = "X-User-Id", defaultValue = "test-user") String userId) {
        log.info("Getting history for user: {}, language: {}, period: {}", userId, targetLanguageCode, period);
        return ResponseEntity.ok(statsService.getMyHistory(userId, targetLanguageCode, period));
    }
}
