package org.wespeak.lesson.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Publisher for lesson events to Kafka.
 * Events are only published if StreamBridge is available (Kafka configured).
 */
@Slf4j
@Service
public class LessonEventPublisher {

    private static final String BINDING = "lessonEvents-out-0";
    private static final String SOURCE = "lesson-service";

    private final StreamBridge streamBridge;

    @Autowired(required = false)
    public LessonEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
        if (streamBridge == null) {
            log.warn("StreamBridge not available - Kafka events will not be published");
        }
    }

    public void publishLessonStarted(
            String userId,
            String lessonId,
            String lessonTitle,
            String lessonType,
            String targetLanguageCode,
            String unitId,
            String courseId) {
        
        Map<String, Object> payload = Map.of(
            "userId", userId,
            "lessonId", lessonId,
            "lessonTitle", lessonTitle,
            "lessonType", lessonType,
            "targetLanguageCode", targetLanguageCode,
            "unitId", unitId != null ? unitId : "",
            "courseId", courseId != null ? courseId : ""
        );
        
        publish("lesson.started", payload, userId);
        log.info("Published lesson.started event for userId: {}, lessonId: {}", userId, lessonId);
    }

    public void publishLessonCompleted(
            String userId,
            String lessonId,
            String lessonTitle,
            String lessonType,
            String targetLanguageCode,
            String unitId,
            String courseId,
            int score,
            int xpEarned,
            int correctAnswers,
            int totalExercises,
            int timeSpentSeconds,
            int attemptNumber,
            boolean isFirstCompletion) {
        
        Map<String, Object> payload = Map.ofEntries(
            Map.entry("userId", userId),
            Map.entry("lessonId", lessonId),
            Map.entry("lessonTitle", lessonTitle),
            Map.entry("lessonType", lessonType),
            Map.entry("targetLanguageCode", targetLanguageCode),
            Map.entry("unitId", unitId != null ? unitId : ""),
            Map.entry("courseId", courseId != null ? courseId : ""),
            Map.entry("score", score),
            Map.entry("xpEarned", xpEarned),
            Map.entry("correctAnswers", correctAnswers),
            Map.entry("totalExercises", totalExercises),
            Map.entry("timeSpentSeconds", timeSpentSeconds),
            Map.entry("attemptNumber", attemptNumber),
            Map.entry("isFirstCompletion", isFirstCompletion)
        );
        
        publish("lesson.completed", payload, userId);
        log.info("Published lesson.completed event for userId: {}, lessonId: {}, score: {}", userId, lessonId, score);
    }

    public void publishUnitCompleted(
            String userId,
            String unitId,
            String unitTitle,
            String courseId,
            String targetLanguageCode,
            int totalLessons,
            int averageScore,
            int totalXPEarned) {
        
        Map<String, Object> payload = Map.of(
            "userId", userId,
            "unitId", unitId,
            "unitTitle", unitTitle,
            "courseId", courseId != null ? courseId : "",
            "targetLanguageCode", targetLanguageCode,
            "totalLessons", totalLessons,
            "averageScore", averageScore,
            "totalXPEarned", totalXPEarned
        );
        
        publish("unit.completed", payload, userId);
        log.info("Published unit.completed event for userId: {}, unitId: {}", userId, unitId);
    }

    public void publishCourseCompleted(
            String userId,
            String courseId,
            String courseTitle,
            String level,
            String targetLanguageCode,
            int totalUnits,
            int totalLessons,
            int averageScore,
            int totalXPEarned) {
        
        Map<String, Object> payload = Map.ofEntries(
            Map.entry("userId", userId),
            Map.entry("courseId", courseId),
            Map.entry("courseTitle", courseTitle),
            Map.entry("level", level),
            Map.entry("targetLanguageCode", targetLanguageCode),
            Map.entry("totalUnits", totalUnits),
            Map.entry("totalLessons", totalLessons),
            Map.entry("averageScore", averageScore),
            Map.entry("totalXPEarned", totalXPEarned)
        );
        
        publish("course.completed", payload, userId);
        log.info("Published course.completed event for userId: {}, courseId: {}", userId, courseId);
    }

    private void publish(String eventType, Map<String, Object> payload, String partitionKey) {
        if (streamBridge == null) {
            log.debug("StreamBridge not available - skipping {} event", eventType);
            return;
        }

        Map<String, Object> event = Map.of(
            "eventType", eventType,
            "version", "1.0",
            "timestamp", Instant.now().toString(),
            "payload", payload,
            "metadata", Map.of(
                "correlationId", UUID.randomUUID().toString(),
                "source", SOURCE
            )
        );

        try {
            Message<Map<String, Object>> message = MessageBuilder
                .withPayload(event)
                .setHeader("partitionKey", partitionKey)
                .build();

            boolean sent = streamBridge.send(BINDING, message);
            if (!sent) {
                log.warn("Failed to send {} event", eventType);
            }
        } catch (Exception e) {
            log.error("Error publishing {} event: {}", eventType, e.getMessage(), e);
        }
    }
}
