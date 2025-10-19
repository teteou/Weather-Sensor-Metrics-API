package com.weathersensor.api.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a single metric data point from a sensor.
 *
 * This is a time-series entity that stores measurements with their timestamp.
 * Each record represents one measurement of one metric type at a specific point in time.
 */
@Entity
@Table(name = "metric_data", indexes = {
        @Index(name = "idx_metric_data_composite",
                columnList = "sensor_id, metric_type, timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "sensor")
public class MetricData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sensor_id", nullable = false)
    private Sensor sensor;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 50)
    private MetricType metricType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Convenience method to get the sensor ID without loading the entire sensor entity.
     */
    public Long getSensorId() {
        return sensor != null ? sensor.getId() : null;
    }
}