package com.weathersensor.api.application.service;

import com.weathersensor.api.application.dto.request.MetricDataRequest;
import com.weathersensor.api.application.dto.response.MetricDataResponse;
import com.weathersensor.api.domain.model.MetricData;
import com.weathersensor.api.domain.model.Sensor;
import com.weathersensor.api.domain.repository.MetricDataRepository;
import com.weathersensor.api.domain.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for ingesting metric data from sensors.
 *
 * Handles validation and persistence of new metric measurements.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetricIngestionService {

    private final MetricDataRepository metricDataRepository;
    private final SensorRepository sensorRepository;

    /**
     * Ingest a new metric data point from a sensor.
     *
     * @param request the metric data to ingest
     * @return the persisted metric data
     * @throws IllegalArgumentException if sensor does not exist
     */
    @Transactional
    public MetricDataResponse ingestMetricData(MetricDataRequest request) {
        log.debug("Ingesting metric data: sensorId={}, type={}, value={}, timestamp={}",
                request.getSensorId(), request.getMetricType(),
                request.getValue(), request.getTimestamp());

        // Validate sensor exists
        Sensor sensor = sensorRepository.findById(request.getSensorId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sensor not found with ID: " + request.getSensorId()));

        // Build and save metric data
        MetricData metricData = MetricData.builder()
                .sensor(sensor)
                .metricType(request.getMetricType())
                .value(request.getValue())
                .timestamp(request.getTimestamp())
                .build();

        MetricData savedMetric = metricDataRepository.save(metricData);

        log.info("Successfully ingested metric data: id={}, sensorCode={}, type={}",
                savedMetric.getId(), sensor.getSensorCode(), savedMetric.getMetricType());

        // Map to response DTO
        return mapToResponse(savedMetric, sensor);
    }

    /**
     * Batch ingest multiple metric data points.
     *
     * This method is optimized for bulk ingestion.
     *
     * @param requests list of metric data to ingest
     * @return list of persisted metrics
     */
    @Transactional
    public java.util.List<MetricDataResponse> ingestMetricDataBatch(
            java.util.List<MetricDataRequest> requests) {

        log.info("Batch ingesting {} metric data points", requests.size());

        return requests.stream()
                .map(this::ingestMetricData)
                .toList();
    }

    /**
     * Map MetricData entity to response DTO.
     */
    private MetricDataResponse mapToResponse(MetricData metricData, Sensor sensor) {
        return MetricDataResponse.builder()
                .id(metricData.getId())
                .sensorId(sensor.getId())
                .sensorCode(sensor.getSensorCode())
                .metricType(metricData.getMetricType())
                .value(metricData.getValue())
                .unit(metricData.getMetricType().getUnit())
                .timestamp(metricData.getTimestamp())
                .createdAt(metricData.getCreatedAt())
                .build();
    }
}