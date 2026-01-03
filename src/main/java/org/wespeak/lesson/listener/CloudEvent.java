package org.wespeak.lesson.listener;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * CloudEvent wrapper for Kafka messages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudEvent<T> {
    private String eventType;
    private String version;
    private Instant timestamp;
    private T payload;
    private Metadata metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata {
        private String correlationId;
        private String source;
    }
}
