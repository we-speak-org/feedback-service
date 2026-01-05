package org.wespeak.feedback.controller;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wespeak.feedback.event.RecordingUploadedPayload;
import org.wespeak.feedback.service.TranscriptionService;

@Slf4j
@RestController
@RequestMapping("/api/v1/seed")
@RequiredArgsConstructor
public class SeedController {

  private final TranscriptionService transcriptionService;

  @PostMapping
  public ResponseEntity<Map<String, String>> seedData() {
    log.info("Seeding test data...");

    // Simulate a recording.uploaded event
    RecordingUploadedPayload payload =
        RecordingUploadedPayload.builder()
            .recordingId("rec-test-123")
            .sessionId("session-test-456")
            .participantId("test-user")
            .targetLanguageCode("en")
            .audioUrl("s3://test-bucket/test-recording.webm")
            .duration(120)
            .format("webm")
            .size(2500000L)
            .build();

    // Process the recording (will create transcript and feedback)
    transcriptionService.processRecording(payload);

    return ResponseEntity.ok(
        Map.of(
            "message", "Test data seeded successfully",
            "sessionId", payload.getSessionId(),
            "participantId", payload.getParticipantId()));
  }
}
