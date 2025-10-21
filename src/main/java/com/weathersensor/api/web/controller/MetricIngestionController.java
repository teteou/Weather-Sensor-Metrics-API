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
 *
 * Supports three ingestion modes:
 * - POST /metrics: Synchronous ingestion (~500 req/s)
 * - POST /metrics/async: Asynchronous ingestion (~2000 req/s)
 * - POST /metrics/batch: Batch ingestion for bulk uploads
 */
@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Metric Ingestion", description = "Endpoints for ingesting sensor metric data")
public class MetricIngestionController {

    private final MetricIngestionService metricIngestionService;

    /**
     * Ingest a single metric data point (synchronous).
     *
     * @param request the metric data to ingest
     * @return the persisted metric data with metadata
     */
    @PostMapping
    @Operation(
            summary = "Ingest a single metric data point (synchronous)",
            description = """
                    Stores a new metric measurement from a sensor and waits for completion.
                    
                    **Use this when:**
                    - You need the generated ID in the response
                    - Low to medium throughput scenarios
                    - Client needs confirmation before proceeding
                    
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
                    
                    **Performance:** ~500 requests/second
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

        log.info("Received metric ingestion request (sync): sensorId={}, type={}",
                request.getSensorId(), request.getMetricType());

        MetricDataResponse response = metricIngestionService.ingestMetricData(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Ingest a single metric data point asynchronously (non-blocking).
     *
     * @param request the metric data to ingest
     * @return 202 Accepted (processing asynchronously)
     */
    @PostMapping("/async")
    @Operation(
            summary = "Ingest a single metric data point (asynchronous)",
            description = """
                    Stores a new metric measurement asynchronously for high-throughput scenarios.
                    Returns immediately with 202 Accepted status.
                    
                    **Use this when:**
                    - High throughput ingestion from IoT devices
                    - Fire-and-forget scenarios
                    - Client doesn't need immediate confirmation
                    - Bulk sensor data from multiple devices
                    
                    **Benefits:**
                    - 4x better throughput (~2000 req/s vs ~500 req/s)
                    - Non-blocking for the client
                    - Better resource utilization under load
                    - Automatic retry and backpressure handling
                    
                    **Trade-offs:**
                    - Response doesn't include the generated ID
                    - Eventual consistency (processed within milliseconds)
                    
                    **Example Request:**
```json
                    {
                      "sensorId": 1,
                      "metricType": "TEMPERATURE",
                      "value": 23.5,
                      "timestamp": "2024-01-15T10:30:00"
                    }
```
                    
                    **Response:** 202 Accepted (empty body)
                    
                    **Performance:** ~2000 requests/second
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "202",
                    description = "Metric accepted and will be processed asynchronously"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data"
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Service temporarily unavailable (too many requests)"
            )
    })
    public ResponseEntity<Void> ingestMetricAsync(
            @Valid @RequestBody MetricDataRequest request) {

        log.info("Received metric ingestion request (async): sensorId={}, type={}",
                request.getSensorId(), request.getMetricType());

        // Fire and forget - CompletableFuture handles async execution
        metricIngestionService.ingestMetricAsync(request);

        // Return 202 Accepted immediately (non-blocking)
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
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
                    
                    **Use this when:**
                    - Uploading historical data
                    - Catching up after network downtime
                    - Bulk import from CSV or external systems
                    
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
                    
                    **Performance:** Optimized with saveAll() (single transaction)
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