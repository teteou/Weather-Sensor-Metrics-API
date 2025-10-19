package com.weathersensor.api.application.dto.response;

import com.weathersensor.api.domain.model.SensorStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO representing a sensor.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Sensor information")
public class SensorResponse {

    @Schema(description = "Sensor unique identifier", example = "1")
    private Long id;

    @Schema(description = "Sensor unique code", example = "SENSOR-001")
    private String sensorCode;

    @Schema(description = "Physical location of the sensor", example = "Madrid - Centro")
    private String location;

    @Schema(description = "Current operational status", example = "ACTIVE")
    private SensorStatus status;

    @Schema(description = "Timestamp when the sensor was created")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the sensor was last updated")
    private LocalDateTime updatedAt;
}