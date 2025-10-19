package com.weathersensor.api.application.service;

import com.weathersensor.api.application.dto.request.MetricQueryRequest;
import com.weathersensor.api.application.dto.response.AggregatedMetricResponse;
import com.weathersensor.api.domain.model.MetricData;
import com.weathersensor.api.domain.model.MetricType;
import com.weathersensor.api.domain.repository.MetricDataRepository;
import com.weathersensor.api.domain.specification.MetricDataSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for querying and aggregating metric data.
 *
 * Supports flexible filtering and statistical operations on time-series data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MetricQueryService {

    private final MetricDataRepository metricDataRepository;

    /**
     * Query aggregated metrics with flexible filtering.
     *
     * @param request query criteria including sensors, metrics, date range, and statistic
     * @return list of aggregated results, one per metric type
     */
    public List<AggregatedMetricResponse> queryAggregatedMetrics(MetricQueryRequest request) {
        log.info("Executing aggregated query: sensors={}, metrics={}, statistic={}, range={} to {}",
                request.getSensorIds(), request.getMetricTypes(),
                request.getStatistic(), request.getStartDate(), request.getEndDate());

        // Apply default date range if not specified (last 7 days)
        LocalDateTime startDate = request.getStartDate() != null
                ? request.getStartDate()
                : LocalDateTime.now().minusDays(7);

        LocalDateTime endDate = request.getEndDate() != null
                ? request.getEndDate()
                : LocalDateTime.now();

        // Validate date range (business rule: 1 day to 1 month)
        validateDateRange(startDate, endDate);

        // Execute aggregation query in database
        List<Object[]> results = metricDataRepository.calculateAggregatedStatistics(
                request.getSensorIds(),
                request.getMetricTypes(),
                startDate,
                endDate,
                request.getStatistic().name()
        );

        // Count data points for each metric type
        Long totalDataPoints = countDataPoints(
                request.getSensorIds(),
                request.getMetricTypes(),
                startDate,
                endDate);

        log.info("Query returned {} aggregated results from {} data points",
                results.size(), totalDataPoints);

        // Map results to response DTOs
        return results.stream()
                .map(result -> mapToAggregatedResponse(
                        result,
                        request,
                        startDate,
                        endDate,
                        totalDataPoints))
                .toList();
    }

    /**
     * Query raw metric data with filtering (no aggregation).
     *
     * @param sensorIds list of sensor IDs (null = all)
     * @param metricTypes list of metric types
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of metric data points
     */
    public List<MetricData> queryRawMetricData(
            List<Long> sensorIds,
            List<MetricType> metricTypes,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        log.debug("Querying raw metric data: sensors={}, metrics={}, range={} to {}",
                sensorIds, metricTypes, startDate, endDate);

        Specification<MetricData> spec = MetricDataSpecification.withFilters(
                sensorIds, metricTypes, startDate, endDate);

        return metricDataRepository.findAll(spec);
    }

    /**
     * Validate that the date range is between 1 day and 1 month.
     */
    private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Start date must be before end date");
        }

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);

        if (daysBetween < 1) {
            throw new IllegalArgumentException(
                    "Date range must be at least 1 day");
        }

        if (daysBetween > 31) {
            throw new IllegalArgumentException(
                    "Date range cannot exceed 1 month (31 days)");
        }
    }

    /**
     * Count total data points matching the query criteria.
     */
    private Long countDataPoints(
            List<Long> sensorIds,
            List<MetricType> metricTypes,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        Specification<MetricData> spec = MetricDataSpecification.withFilters(
                sensorIds, metricTypes, startDate, endDate);

        return metricDataRepository.count(spec);
    }

    /**
     * Map database result to aggregated response DTO.
     */
    private AggregatedMetricResponse mapToAggregatedResponse(
            Object[] result,
            MetricQueryRequest request,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long dataPointsCount) {

        MetricType metricType = (MetricType) result[0];
        BigDecimal value = (BigDecimal) result[1];

        return AggregatedMetricResponse.builder()
                .metricType(metricType)
                .value(value)
                .unit(metricType.getUnit())
                .statistic(request.getStatistic())
                .startDate(startDate)
                .endDate(endDate)
                .sensorIds(request.getSensorIds())
                .dataPointsCount(dataPointsCount)
                .build();
    }
}