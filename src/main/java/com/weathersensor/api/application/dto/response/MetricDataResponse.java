package com.weathersensor.api.application.dto.response;

import com.weathersensor.api.domain.model.MetricType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO representing a single metric data point.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "A single metric measurement from a sensor")
public class MetricDataResponse {

    @Schema(description = "Metric data unique identifier", example = "1")
    private Long id;

    @Schema(description = "ID of the sensor that collected this metric", example = "1")
    private Long sensorId;

    @Schema(description = "Sensor code", example = "SENSOR-001")
    private String sensorCode;

    @Schema(description = "Type of metric", example = "TEMPERATURE")
    private MetricType metricType;

    @Schema(description = "Measured value", example = "23.5")
    private BigDecimal value;

    @Schema(description = "Unit of measurement", example = "°C")
    private String unit;

    @Schema(description = "Timestamp when the measurement was taken")
    private LocalDateTime timestamp;

    @Schema(description = "Timestamp when the record was created in the database")
    private LocalDateTime createdAt;
}