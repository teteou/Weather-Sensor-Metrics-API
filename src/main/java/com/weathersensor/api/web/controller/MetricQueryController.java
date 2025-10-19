package com.weathersensor.api.web.controller;

import com.weathersensor.api.application.dto.request.MetricQueryRequest;
import com.weathersensor.api.application.dto.response.AggregatedMetricResponse;
import com.weathersensor.api.application.service.MetricQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for querying and aggregating metric data.
 */
@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Metric Query", description = "Endpoints for querying and analyzing sensor metrics")
public class MetricQueryController {

    private final MetricQueryService metricQueryService;

    /**
     * Query aggregated metrics with flexible filtering.
     *
     * @param request query criteria
     * @return aggregated results
     */
    @PostMapping("/query")
    @Operation(
            summary = "Query aggregated metrics with filters",
            description = """
                    Retrieves aggregated statistics for sensor metrics based on flexible criteria.
                    
                    **Example 1: Average temperature for all sensors in the last week**
```json
                    {
                      "metricTypes": ["TEMPERATURE"],
                      "statistic": "AVG"
                    }
```
                    
                    **Example 2: Max temperature and humidity for specific sensors**
```json
                    {
                      "sensorIds": [1, 2],
                      "metricTypes": ["TEMPERATURE", "HUMIDITY"],
                      "statistic": "MAX",
                      "startDate": "2024-01-08T00:00:00",
                      "endDate": "2024-01-15T23:59:59"
                    }
```
                    
                    **Date Range Rules:**
                    - Minimum: 1 day
                    - Maximum: 31 days (1 month)
                    - Default: Last 7 days if not specified
                    
                    **Supported Statistics:**
                    - MIN: Minimum value
                    - MAX: Maximum value
                    - AVG: Average (mean) value
                    - SUM: Sum of all values
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Query executed successfully",
                    content = @Content(schema = @Schema(implementation = AggregatedMetricResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid query parameters (e.g., date range exceeds limit)"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<List<AggregatedMetricResponse>> queryMetrics(
            @Valid @RequestBody MetricQueryRequest request) {

        log.info("Received metric query request: sensors={}, metrics={}, statistic={}",
                request.getSensorIds(), request.getMetricTypes(), request.getStatistic());

        List<AggregatedMetricResponse> results = metricQueryService.queryAggregatedMetrics(request);

        log.info("Query returned {} results", results.size());

        return ResponseEntity.ok(results);
    }
}