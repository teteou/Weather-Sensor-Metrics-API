package com.weathersensor.api.domain.repository;

import com.weathersensor.api.domain.model.MetricData;
import com.weathersensor.api.domain.model.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for accessing MetricData entities with support for dynamic queries.
 */
@Repository
public interface MetricDataRepository extends
        JpaRepository<MetricData, Long>,
        JpaSpecificationExecutor<MetricData> {

    /**
     * Calculate aggregated statistics for given sensors and metrics within a date range.
     *
     * This method performs database-level aggregation for optimal performance.
     *
     * @param sensorIds list of sensor IDs (null means all sensors)
     * @param metricTypes list of metric types to aggregate
     * @param startDate start of the date range
     * @param endDate end of the date range
     * @param statistic type of statistic (MIN, MAX, AVG, SUM)
     * @return list of Object arrays containing [MetricType, aggregated value]
     */
    @Query("""
        SELECT m.metricType,
               CASE WHEN :statistic = 'MIN' THEN MIN(m.value)
                    WHEN :statistic = 'MAX' THEN MAX(m.value)
                    WHEN :statistic = 'SUM' THEN SUM(m.value)
                    ELSE AVG(m.value)
               END as result
        FROM MetricData m
        WHERE (:sensorIds IS NULL OR m.sensor.id IN :sensorIds)
          AND m.metricType IN :metricTypes
          AND m.timestamp BETWEEN :startDate AND :endDate
        GROUP BY m.metricType
        """)
    List<Object[]> calculateAggregatedStatistics(
            @Param("sensorIds") List<Long> sensorIds,
            @Param("metricTypes") List<MetricType> metricTypes,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("statistic") String statistic
    );

    /**
     * Find the latest metric data for a specific sensor and metric type.
     *
     * @param sensorId the sensor ID
     * @param metricType the metric type
     * @return the most recent metric data
     */
    @Query("""
        SELECT m FROM MetricData m
        WHERE m.sensor.id = :sensorId
          AND m.metricType = :metricType
        ORDER BY m.timestamp DESC
        LIMIT 1
        """)
    MetricData findLatestBySensorAndMetricType(
            @Param("sensorId") Long sensorId,
            @Param("metricType") MetricType metricType
    );

    /**
     * Count total metric data points for a sensor within a date range.
     *
     * @param sensorId the sensor ID
     * @param startDate start of the date range
     * @param endDate end of the date range
     * @return count of data points
     */
    @Query("""
        SELECT COUNT(m) FROM MetricData m
        WHERE m.sensor.id = :sensorId
          AND m.timestamp BETWEEN :startDate AND :endDate
        """)
    long countBySensorAndDateRange(
            @Param("sensorId") Long sensorId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}