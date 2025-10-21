package com.weathersensor.api.application.listener;

import com.weathersensor.api.domain.event.MetricIngestedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener for metric ingestion events.
 *
 * Processes events asynchronously to avoid blocking the ingestion flow.
 *
 * Current implementation: Audit logging
 *
 * Future enhancements:
 * - Send alerts for anomalous values (e.g., temperature > 50°C)
 * - Update real-time dashboards via WebSocket
 * - Publish to external systems (Kafka, SQS)
 * - Invalidate query caches
 * - Aggregate metrics for analytics
 */
@Component
@Slf4j
public class MetricIngestedEventListener {

    /**
     * Handle metric ingestion event asynchronously.
     *
     * @Async ensures this runs in a separate thread, not blocking the ingestion
     */
    @Async
    @EventListener
    public void handleMetricIngested(MetricIngestedEvent event) {
        log.info("Metric ingested event received: sensor={}, type={}, value={}, timestamp={}",
                event.getMetricData().getSensor().getId(),
                event.getMetricData().getMetricType(),
                event.getMetricData().getValue(),
                event.getMetricData().getTimestamp());

        // Future: Add business logic here
        // - Check thresholds and send alerts
        // - Update aggregated statistics
        // - Publish to message queue
        // - Invalidate caches

        // Example: Alert if temperature too high
        /*
        if (event.getMetricData().getMetricType() == MetricType.TEMPERATURE &&
            event.getMetricData().getValue().compareTo(new BigDecimal("50")) > 0) {
            log.warn("ALERT: High temperature detected: {} at sensor {}",
                    event.getMetricData().getValue(),
                    event.getMetricData().getSensor().getId());
            // Send notification...
        }
        */
    }
}