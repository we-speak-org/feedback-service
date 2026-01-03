package org.wespeak.lesson.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.wespeak.lesson.service.ProgressService;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Kafka event listeners for user events.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class UserEventListener {

    private final ProgressService progressService;
    private final ObjectMapper objectMapper;

    /**
     * Listener for user.registered events.
     * Creates initial progress for each learning profile.
     */
    @Bean
    public Consumer<String> userRegisteredListener() {
        return message -> {
            try {
                log.debug("Received message on user.events: {}", message);
                
                // Parse the CloudEvent
                Map<String, Object> eventMap = objectMapper.readValue(message, new TypeReference<>() {});
                String eventType = (String) eventMap.get("eventType");
                
                if (!"user.registered".equals(eventType)) {
                    log.debug("Ignoring event type: {}", eventType);
                    return;
                }
                
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = (Map<String, Object>) eventMap.get("payload");
                if (payload == null) {
                    payload = eventMap; // Fallback if payload is at root level
                }
                
                String userId = (String) payload.get("userId");
                @SuppressWarnings("unchecked")
                var learningProfiles = (java.util.List<Map<String, Object>>) payload.get("learningProfiles");
                
                if (userId == null || learningProfiles == null) {
                    log.warn("Invalid user.registered event: missing userId or learningProfiles");
                    return;
                }
                
                log.info("Processing user.registered for userId: {}", userId);
                
                for (Map<String, Object> profile : learningProfiles) {
                    String languageCode = (String) profile.get("targetLanguageCode");
                    if (languageCode != null) {
                        progressService.initializeProgress(userId, languageCode);
                    }
                }
                
                log.info("Successfully processed user.registered for userId: {}", userId);
                
            } catch (Exception e) {
                log.error("Error processing user.registered event: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to process user.registered event", e);
            }
        };
    }

    /**
     * Listener for learning_profile.created events.
     * Creates progress for a new language.
     */
    @Bean
    public Consumer<String> learningProfileCreatedListener() {
        return message -> {
            try {
                log.debug("Received message on user.events: {}", message);
                
                // Parse the CloudEvent
                Map<String, Object> eventMap = objectMapper.readValue(message, new TypeReference<>() {});
                String eventType = (String) eventMap.get("eventType");
                
                if (!"learning_profile.created".equals(eventType)) {
                    log.debug("Ignoring event type: {}", eventType);
                    return;
                }
                
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = (Map<String, Object>) eventMap.get("payload");
                if (payload == null) {
                    payload = eventMap;
                }
                
                String userId = (String) payload.get("userId");
                String languageCode = (String) payload.get("targetLanguageCode");
                
                if (userId == null || languageCode == null) {
                    log.warn("Invalid learning_profile.created event: missing userId or languageCode");
                    return;
                }
                
                log.info("Processing learning_profile.created for userId: {}, language: {}", userId, languageCode);
                progressService.initializeProgress(userId, languageCode);
                log.info("Successfully processed learning_profile.created");
                
            } catch (Exception e) {
                log.error("Error processing learning_profile.created event: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to process learning_profile.created event", e);
            }
        };
    }
}
