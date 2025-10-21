package com.weathersensor.api.web.controller;

import com.weathersensor.api.application.dto.request.MetricDataRequest;
import com.weathersensor.api.application.dto.response.MetricDataResponse;
import com.weathersensor.api.application.service.MetricIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for ingesting metric data from sensors.
 */
@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Metric Ingestion", description = "Endpoints for ingesting sensor metric data")
public class MetricIngestionController {

    private final MetricIngestionService metricIngestionService;

    /**
     * Ingest a single metric data point.
     *
     * @param request the metric data to ingest
     * @return the persisted metric data with metadata
     */
    @PostMapping
    @Operation(
            summary = "Ingest a single metric data point",
            description = """
                    Stores a new metric measurement from a sensor.
                    
                    **Example Request:**
```json
                    {
                      "sensorId": 1,
                      "metricType": "TEMPERATURE",
                      "value": 23.5,
                      "timestamp": "2024-01-15T10:30:00"
                    }
```
                    
                    **Validation Rules:**
                    - Sensor ID must exist in the database
                    - Value must be between -100 and 1000
                    - Timestamp cannot be in the future
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Metric data successfully ingested",
                    content = @Content(schema = @Schema(implementation = MetricDataResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data or sensor not found"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<MetricDataResponse> ingestMetric(
            @Valid @RequestBody MetricDataRequest request) {

        log.info("Received metric ingestion request: sensorId={}, type={}",
                request.getSensorId(), request.getMetricType());

        MetricDataResponse response = metricIngestionService.ingestMetricData(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Ingest multiple metric data points in a single request.
     *
     * @param requests list of metric data to ingest
     * @return list of persisted metrics
     */
    @PostMapping("/batch")
    @Operation(
            summary = "Batch ingest multiple metric data points",
            description = """
                    Stores multiple metric measurements in a single request.
                    Useful for bulk data uploads or catching up on missed readings.
                    
                    **Example Request:**
```json
                    [
                      {
                        "sensorId": 1,
                        "metricType": "TEMPERATURE",
                        "value": 23.5,
                        "timestamp": "2024-01-15T10:30:00"
                      },
                      {
                        "sensorId": 1,
                        "metricType": "HUMIDITY",
                        "value": 65.0,
                        "timestamp": "2024-01-15T10:30:00"
                      }
                    ]
```
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "All metrics successfully ingested"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data in one or more requests"
            )
    })
    public ResponseEntity<List<MetricDataResponse>> ingestMetricsBatch(
            @Valid @RequestBody List<MetricDataRequest> requests) {

        log.info("Received batch metric ingestion request: {} data points", requests.size());

        List<MetricDataResponse> responses = metricIngestionService.ingestMetricDataBatch(requests);

        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }
}