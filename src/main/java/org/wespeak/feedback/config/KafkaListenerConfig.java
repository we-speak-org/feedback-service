package org.wespeak.feedback.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.wespeak.feedback.service.TranscriptionService;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaListenerConfig {

  private final TranscriptionService transcriptionService;

  // STUBBED: Kafka integration is disabled for this version
  // In production, this would be:
  // @Bean
  // public Consumer<CloudEvent<RecordingUploadedPayload>> recordingUploadedListener() {
  //     return event -> {
  //         log.info("Received recording.uploaded event: {}", event.getData());
  //         transcriptionService.processRecording(event.getData());
  //     };
  // }

  // For now, we use the /api/v1/seed endpoint to trigger processing manually
}
