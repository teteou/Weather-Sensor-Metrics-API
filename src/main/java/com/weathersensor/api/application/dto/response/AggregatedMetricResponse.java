package com.weathersensor.api.application.dto.response;

import com.weathersensor.api.application.dto.request.MetricQueryRequest;
import com.weathersensor.api.domain.model.MetricType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO representing aggregated metric statistics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Aggregated statistical result for a metric type")
public class AggregatedMetricResponse {

    @Schema(description = "Type of metric aggregated", example = "TEMPERATURE")
    private MetricType metricType;

    @Schema(description = "Aggregated value", example = "22.35")
    private BigDecimal value;

    @Schema(description = "Unit of measurement", example = "°C")
    private String unit;

    @Schema(description = "Type of statistic applied", example = "AVG")
    private MetricQueryRequest.StatisticType statistic;

    @Schema(description = "Start of the date range used for aggregation")
    private LocalDateTime startDate;

    @Schema(description = "End of the date range used for aggregation")
    private LocalDateTime endDate;

    @Schema(description = "List of sensor IDs included in the aggregation (null = all sensors)")
    private List<Long> sensorIds;

    @Schema(description = "Number of data points used in the aggregation", example = "720")
    private Long dataPointsCount;
}
