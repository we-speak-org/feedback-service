package org.wespeak.feedback.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.wespeak.feedback.dto.*;
import org.wespeak.feedback.entity.Feedback;
import org.wespeak.feedback.entity.Transcript;
import org.wespeak.feedback.exception.ForbiddenException;
import org.wespeak.feedback.exception.ResourceNotFoundException;
import org.wespeak.feedback.repository.FeedbackRepository;
import org.wespeak.feedback.repository.TranscriptRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {
    
    private final FeedbackRepository feedbackRepository;
    private final TranscriptRepository transcriptRepository;
    
    public TranscriptResponse getTranscript(String transcriptId, String userId) {
        Transcript transcript = transcriptRepository.findById(transcriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Transcript not found"));
        
        if (!transcript.getParticipantId().equals(userId)) {
            throw new ForbiddenException("Access denied to this transcript");
        }
        
        return mapToTranscriptResponse(transcript);
    }
    
    public List<TranscriptResponse> getTranscriptsBySession(String sessionId, String userId) {
        List<Transcript> transcripts = transcriptRepository.findBySessionId(sessionId);
        
        return transcripts.stream()
                .filter(t -> t.getParticipantId().equals(userId))
                .map(this::mapToTranscriptResponse)
                .collect(Collectors.toList());
    }
    
    public FeedbackResponse getFeedback(String feedbackId, String userId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found"));
        
        if (!feedback.getUserId().equals(userId)) {
            throw new ForbiddenException("Access denied to this feedback");
        }
        
        return mapToFeedbackResponse(feedback);
    }
    
    public FeedbackListResponse getMyFeedbacks(String userId, String languageCode, Pageable pageable) {
        Page<Feedback> feedbackPage;
        
        if (languageCode != null && !languageCode.isEmpty()) {
            feedbackPage = feedbackRepository.findByUserIdAndTargetLanguageCode(userId, languageCode, pageable);
        } else {
            feedbackPage = feedbackRepository.findByUserId(userId, pageable);
        }
        
        List<FeedbackListItem> items = feedbackPage.getContent().stream()
                .map(this::mapToFeedbackListItem)
                .collect(Collectors.toList());
        
        return FeedbackListResponse.builder()
                .items(items)
                .page(feedbackPage.getNumber())
                .size(feedbackPage.getSize())
                .total(feedbackPage.getTotalElements())
                .totalPages(feedbackPage.getTotalPages())
                .build();
    }
    
    public FeedbackResponse getFeedbackBySession(String sessionId, String userId) {
        Feedback feedback = feedbackRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found for this session"));
        
        return mapToFeedbackResponse(feedback);
    }
    
    private TranscriptResponse mapToTranscriptResponse(Transcript transcript) {
        return TranscriptResponse.builder()
                .id(transcript.getId())
                .sessionId(transcript.getSessionId())
                .participantId(transcript.getParticipantId())
                .targetLanguageCode(transcript.getTargetLanguageCode())
                .content(transcript.getContent())
                .segments(transcript.getSegments())
                .duration(transcript.getDuration())
                .wordCount(transcript.getWordCount())
                .confidence(transcript.getConfidence())
                .status(transcript.getStatus())
                .createdAt(transcript.getCreatedAt())
                .completedAt(transcript.getCompletedAt())
                .build();
    }
    
    private FeedbackResponse mapToFeedbackResponse(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .transcriptId(feedback.getTranscriptId())
                .userId(feedback.getUserId())
                .sessionId(feedback.getSessionId())
                .targetLanguageCode(feedback.getTargetLanguageCode())
                .overallScore(feedback.getOverallScore())
                .grammarScore(feedback.getGrammarScore())
                .vocabularyScore(feedback.getVocabularyScore())
                .fluencyScore(feedback.getFluencyScore())
                .pronunciationScore(feedback.getPronunciationScore())
                .errors(feedback.getErrors())
                .strengths(feedback.getStrengths())
                .improvements(feedback.getImprovements())
                .summary(feedback.getSummary())
                .xpAwarded(feedback.getXpAwarded())
                .status(feedback.getStatus())
                .createdAt(feedback.getCreatedAt())
                .completedAt(feedback.getCompletedAt())
                .build();
    }
    
    private FeedbackListItem mapToFeedbackListItem(Feedback feedback) {
        return FeedbackListItem.builder()
                .id(feedback.getId())
                .sessionId(feedback.getSessionId())
                .targetLanguageCode(feedback.getTargetLanguageCode())
                .overallScore(feedback.getOverallScore())
                .xpAwarded(feedback.getXpAwarded())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}
