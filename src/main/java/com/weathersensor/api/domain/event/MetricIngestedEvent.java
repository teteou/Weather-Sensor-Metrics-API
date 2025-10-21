package com.weathersensor.api.domain.event;

import com.weathersensor.api.domain.model.MetricData;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Domain event fired when a metric is successfully ingested.
 *
 * Use cases:
 * - Audit logging
 * - Real-time notifications (e.g., alert if temperature > threshold)
 * - Cache invalidation
 * - Publishing to message queue (Kafka, SQS)
 * - Analytics/monitoring
 *
 * Event-driven pattern benefits:
 * - Decouples metric ingestion from side effects
 * - Easy to add new listeners without modifying core logic
 * - Supports async processing of post-ingestion tasks
 */
@Getter
public class MetricIngestedEvent extends ApplicationEvent {

    private final MetricData metricData;
    private final LocalDateTime occurredAt;

    public MetricIngestedEvent(Object source, MetricData metricData) {
        super(source);
        this.metricData = metricData;
        this.occurredAt = LocalDateTime.now();
    }
}