package com.weathersensor.api.application.service;

import com.weathersensor.api.application.dto.request.MetricDataRequest;
import com.weathersensor.api.application.dto.response.MetricDataResponse;
import com.weathersensor.api.application.mapper.MetricMapper;
import com.weathersensor.api.domain.model.MetricData;
import com.weathersensor.api.domain.model.Sensor;
import com.weathersensor.api.domain.repository.MetricDataRepository;
import com.weathersensor.api.domain.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for ingesting metric data from sensors.
 * Handles validation and persistence of new metric measurements.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetricIngestionService {

    private final MetricDataRepository metricDataRepository;
    private final SensorRepository sensorRepository;
    private final MetricMapper metricMapper;

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

        // Map request to entity using MapStruct
        MetricData metricData = metricMapper.toEntity(request);
        metricData.setSensor(sensor);  // Set the sensor relationship

        // Save
        MetricData savedMetric = metricDataRepository.save(metricData);

        log.info("Successfully ingested metric data: id={}, sensorCode={}, type={}",
                savedMetric.getId(), sensor.getSensorCode(), savedMetric.getMetricType());

        // Map entity to response using MapStruct
        return metricMapper.toResponse(savedMetric);
    }

    /**
     * Batch ingest multiple metric data points.
     * Optimized for bulk ingestion using saveAll().
     *
     * @param requests list of metric data to ingest
     * @return list of persisted metrics
     */
    @Transactional
    public List<MetricDataResponse> ingestMetricDataBatch(List<MetricDataRequest> requests) {
        log.info("Batch ingesting {} metric data points", requests.size());

        List<MetricData> metricDataList = new ArrayList<>();

        // Map and validate all requests
        for (MetricDataRequest request : requests) {
            Sensor sensor = sensorRepository.findById(request.getSensorId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Sensor not found with ID: " + request.getSensorId()));

            MetricData metricData = metricMapper.toEntity(request);
            metricData.setSensor(sensor);
            metricDataList.add(metricData);
        }

        // Batch save (single transaction, more efficient)
        List<MetricData> savedMetrics = metricDataRepository.saveAll(metricDataList);

        log.info("Successfully batch ingested {} metric data points", savedMetrics.size());

        // Map all to responses using MapStruct
        return metricMapper.toResponseList(savedMetrics);
    }
}