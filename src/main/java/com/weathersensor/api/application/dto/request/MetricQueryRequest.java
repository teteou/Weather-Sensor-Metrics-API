package com.weathersensor.api.application.dto.request;

import com.weathersensor.api.domain.model.MetricType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for querying aggregated metrics with filters.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Query criteria for retrieving aggregated sensor metrics")
public class MetricQueryRequest {

    @Schema(description = "List of sensor IDs to include (null or empty = all sensors)",
            example = "[1, 2, 3]")
    private List<Long> sensorIds;

    @NotEmpty(message = "At least one metric type must be specified")
    @Schema(description = "List of metric types to query",
            required = true,
            example = "[\"TEMPERATURE\", \"HUMIDITY\"]")
    private List<MetricType> metricTypes;

    @NotNull(message = "Statistic type is required")
    @Schema(description = "Statistical operation to perform on the data",
            required = true,
            example = "AVG")
    private StatisticType statistic;

    @Schema(description = "Start of the date range (default: 7 days ago)",
            example = "2024-01-08T00:00:00")
    private LocalDateTime startDate;

    @Schema(description = "End of the date range (default: now)",
            example = "2024-01-15T23:59:59")
    private LocalDateTime endDate;

    /**
     * Statistical operations supported for aggregation.
     */
    @Schema(description = "Type of statistical aggregation")
    public enum StatisticType {
        MIN,
        MAX,
        AVG,
        SUM
    }
}