package org.wespeak.conversation.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Scheduling configuration that can be disabled via property.
 * In tests, set spring.task.scheduling.enabled=false to disable.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "spring.task.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
