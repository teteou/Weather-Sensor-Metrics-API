package com.weathersensor.api.application.dto.request;

import com.weathersensor.api.domain.model.MetricType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO for ingesting a new metric data point.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to store a new metric data point from a sensor")
public class MetricDataRequest {

    @NotNull(message = "Sensor ID is required")
    @Positive(message = "Sensor ID must be positive")
    @Schema(description = "ID of the sensor that collected this metric",
            example = "1",
            required = true)
    private Long sensorId;

    @NotNull(message = "Metric type is required")
    @Schema(description = "Type of metric being measured",
            required = true,
            example = "TEMPERATURE")
    private MetricType metricType;

    @NotNull(message = "Value is required")
    @DecimalMin(value = "-100.0", message = "Value must be >= -100")
    @DecimalMax(value = "1000.0", message = "Value must be <= 1000")
    @Schema(description = "Measured value",
            example = "23.5",
            required = true)
    private BigDecimal value;

    @NotNull(message = "Timestamp is required")
    @PastOrPresent(message = "Timestamp cannot be in the future")
    @Schema(description = "Timestamp when the measurement was taken",
            example = "2024-01-15T10:30:00",
            required = true)
    private LocalDateTime timestamp;
}