package com.weathersensor.api.infrastructure.health;

import com.weathersensor.api.domain.model.SensorStatus;
import com.weathersensor.api.domain.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for sensor connectivity status.
 *
 * Monitors:
 * - Number of active sensors
 * - Total registered sensors
 * - Inactive sensor count
 *
 * Returns DOWN status if no active sensors are available.
 */
@Component("sensors")
@RequiredArgsConstructor
@Slf4j
public class SensorConnectivityHealthIndicator implements HealthIndicator {

    private final SensorRepository sensorRepository;

    @Override
    public Health health() {
        try {
            long activeSensors = sensorRepository.countByStatus(SensorStatus.ACTIVE);
            long totalSensors = sensorRepository.count();

            log.debug("Sensor health check: {}/{} sensors active",
                    activeSensors, totalSensors);

            // Consider unhealthy if no active sensors
            if (activeSensors == 0 && totalSensors > 0) {
                return Health.down()
                        .withDetail("active_sensors", 0)
                        .withDetail("total_sensors", totalSensors)
                        .withDetail("inactive_sensors", totalSensors)
                        .withDetail("message", "No active sensors available")
                        .build();
            }

            return Health.up()
                    .withDetail("active_sensors", activeSensors)
                    .withDetail("total_sensors", totalSensors)
                    .withDetail("inactive_sensors", totalSensors - activeSensors)
                    .withDetail("availability_percentage",
                            totalSensors > 0 ? (activeSensors * 100.0 / totalSensors) : 0)
                    .build();

        } catch (Exception e) {
            log.error("Sensor health check failed", e);

            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("error_type", e.getClass().getSimpleName())
                    .build();
        }
    }
}