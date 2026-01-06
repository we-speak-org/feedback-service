package org.wespeak.feedback.service;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.wespeak.feedback.entity.Transcript;
import org.wespeak.feedback.entity.TranscriptSegment;
import org.wespeak.feedback.entity.TranscriptStatus;
import org.wespeak.feedback.event.RecordingUploadedPayload;
import org.wespeak.feedback.repository.TranscriptRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptionService {

  private final TranscriptRepository transcriptRepository;
  private final AnalysisService analysisService;
  private final StorageService storageService;

  public void processRecording(RecordingUploadedPayload payload) {
    log.info("Processing recording: {}", payload.getRecordingId());

    // Create Transcript in PENDING status
    Transcript transcript =
        Transcript.builder()
            .sessionId(payload.getSessionId())
            .participantId(payload.getParticipantId())
            .recordingId(payload.getRecordingId())
            .targetLanguageCode(payload.getTargetLanguageCode())
            .duration(payload.getDuration())
            .status(TranscriptStatus.PENDING)
            .build();

    transcript = transcriptRepository.save(transcript);
    log.info("Created transcript: {}", transcript.getId());

    // Start transcription (async simulation)
    transcribeAudio(transcript, payload.getAudioUrl());
  }

  private void transcribeAudio(Transcript transcript, String audioUrl) {
    try {
      log.info("Starting transcription for transcript: {}", transcript.getId());
      transcript.setStatus(TranscriptStatus.PROCESSING);
      transcriptRepository.save(transcript);

      // STUBBED: In production, this would:
      // 1. Download audio from R2
      try (var audioStream = storageService.downloadFile(audioUrl)) {
        log.info("Downloaded audio file for transcript: {}", transcript.getId());
        // 2. Call Whisper API with the audio stream
        // whisperClient.transcribe(audioStream);
      }

      // 3. Parse response

      // For now, create a mock transcription
      String mockContent =
          "Hello, how are you today? I am learning English and practicing my conversation skills. "
              + "Yesterday I go to the park and I see many peoples. It was very nice weather.";

      List<TranscriptSegment> mockSegments =
          List.of(
              TranscriptSegment.builder()
                  .startTime(0.0)
                  .endTime(3.5)
                  .text("Hello, how are you today?")
                  .confidence(0.95)
                  .build(),
              TranscriptSegment.builder()
                  .startTime(3.8)
                  .endTime(8.2)
                  .text("I am learning English and practicing my conversation skills.")
                  .confidence(0.92)
                  .build(),
              TranscriptSegment.builder()
                  .startTime(8.5)
                  .endTime(12.8)
                  .text("Yesterday I go to the park and I see many peoples.")
                  .confidence(0.88)
                  .build(),
              TranscriptSegment.builder()
                  .startTime(13.0)
                  .endTime(15.5)
                  .text("It was very nice weather.")
                  .confidence(0.94)
                  .build());

      transcript.setContent(mockContent);
      transcript.setSegments(mockSegments);
      transcript.setWordCount(mockContent.split("\\s+").length);
      transcript.setConfidence(0.92);
      transcript.setStatus(TranscriptStatus.COMPLETED);
      transcript.setCompletedAt(Instant.now());

      transcript = transcriptRepository.save(transcript);
      log.info("Transcription completed for transcript: {}", transcript.getId());

      // Trigger AI analysis
      analysisService.analyzeTranscript(transcript);

    } catch (Exception e) {
      log.error("Transcription failed for transcript: {}", transcript.getId(), e);
      transcript.setStatus(TranscriptStatus.FAILED);
      transcriptRepository.save(transcript);
    }
  }
}
