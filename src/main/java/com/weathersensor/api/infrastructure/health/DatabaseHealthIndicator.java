package com.weathersensor.api.infrastructure.health;

import com.weathersensor.api.domain.repository.MetricDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Custom health indicator for database connectivity and data availability.
 *
 * Provides detailed health information about:
 * - Database connection status
 * - Total metrics stored
 * - Last check timestamp
 *
 * Used by monitoring systems (Kubernetes readiness probes, AWS health checks, etc.)
 */
@Component("database")
@RequiredArgsConstructor
@Slf4j
public class DatabaseHealthIndicator implements HealthIndicator {

    private final MetricDataRepository metricDataRepository;

    @Override
    public Health health() {
        try {
            // Simple query to verify database connectivity
            long totalMetrics = metricDataRepository.count();

            log.debug("Database health check successful: {} metrics", totalMetrics);

            return Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("status", "Connected")
                    .withDetail("total_metrics", totalMetrics)
                    .withDetail("checked_at", LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Database health check failed", e);

            return Health.down()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("status", "Disconnected")
                    .withDetail("error", e.getMessage())
                    .withDetail("error_type", e.getClass().getSimpleName())
                    .withDetail("checked_at", LocalDateTime.now())
                    .build();
        }
    }
}