package com.weathersensor.api.domain.specification;

import com.weathersensor.api.domain.model.MetricData;
import com.weathersensor.api.domain.model.MetricType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications for building dynamic queries on MetricData.
 *
 * This class provides a type-safe way to construct complex queries
 * with optional filters for sensors, metric types, and date ranges.
 */
public class MetricDataSpecification {

    /**
     * Build a specification with multiple optional filters.
     *
     * @param sensorIds list of sensor IDs to filter by (null = all sensors)
     * @param metricTypes list of metric types to filter by
     * @param startDate start of the date range
     * @param endDate end of the date range
     * @return Specification for the given filters
     */
    public static Specification<MetricData> withFilters(
            List<Long> sensorIds,
            List<MetricType> metricTypes,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by sensor IDs (if specified)
            if (sensorIds != null && !sensorIds.isEmpty()) {
                predicates.add(root.get("sensor").get("id").in(sensorIds));
            }

            // Filter by metric types
            if (metricTypes != null && !metricTypes.isEmpty()) {
                predicates.add(root.get("metricType").in(metricTypes));
            }

            // Date range filter (always required for time-series data)
            if (startDate != null && endDate != null) {
                predicates.add(criteriaBuilder.between(
                        root.get("timestamp"), startDate, endDate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Specification to filter by a single sensor.
     *
     * @param sensorId the sensor ID
     * @return Specification for the sensor
     */
    public static Specification<MetricData> hasSensorId(Long sensorId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("sensor").get("id"), sensorId);
    }

    /**
     * Specification to filter by metric type.
     *
     * @param metricType the metric type
     * @return Specification for the metric type
     */
    public static Specification<MetricData> hasMetricType(MetricType metricType) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("metricType"), metricType);
    }

    /**
     * Specification to filter by date range.
     *
     * @param startDate start of the range
     * @param endDate end of the range
     * @return Specification for the date range
     */
    public static Specification<MetricData> betweenDates(
            LocalDateTime startDate,
            LocalDateTime endDate) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.between(root.get("timestamp"), startDate, endDate);
    }
}