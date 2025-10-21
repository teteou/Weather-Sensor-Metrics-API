package com.weathersensor.api.application.service;

import com.weathersensor.api.application.dto.request.MetricQueryRequest;
import com.weathersensor.api.application.dto.response.AggregatedMetricResponse;
import com.weathersensor.api.domain.model.MetricType;
import com.weathersensor.api.domain.repository.MetricDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MetricQueryService Unit Tests")
class MetricQueryServiceTest {

    @Mock
    private MetricDataRepository metricDataRepository;

    @InjectMocks
    private MetricQueryService metricQueryService;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
        endDate = LocalDateTime.of(2024, 1, 7, 23, 59);
    }

    @Test
    @DisplayName("Should calculate average temperature for single sensor")
    void shouldCalculateAverageTemperatureForSingleSensor() {
        // Given
        MetricQueryRequest request = MetricQueryRequest.builder()
                .sensorIds(List.of(1L))
                .metricTypes(List.of(MetricType.TEMPERATURE))
                .statistic(MetricQueryRequest.StatisticType.AVG)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        // Mock repository response
        List<Object[]> mockResults = new ArrayList<>();
        mockResults.add(new Object[]{MetricType.TEMPERATURE, new BigDecimal("23.50")});

        when(metricDataRepository.calculateAggregatedStatistics(
                anyList(), anyList(), any(LocalDateTime.class),
                any(LocalDateTime.class), anyString()))
                .thenReturn(mockResults);

        // When
        List<AggregatedMetricResponse> results =
                metricQueryService.queryAggregatedMetrics(request);

        // Then
        assertThat(results).hasSize(1);

        AggregatedMetricResponse response = results.get(0);
        assertThat(response.getMetricType()).isEqualTo(MetricType.TEMPERATURE);
        assertThat(response.getValue()).isEqualByComparingTo("23.50");
        assertThat(response.getStatistic()).isEqualTo(MetricQueryRequest.StatisticType.AVG);
        assertThat(response.getStartDate()).isEqualTo(startDate);
        assertThat(response.getEndDate()).isEqualTo(endDate);
        assertThat(response.getSensorIds()).containsExactly(1L);

        // Verify repository was called with correct parameters
        verify(metricDataRepository).calculateAggregatedStatistics(
                eq(List.of(1L)),
                eq(List.of(MetricType.TEMPERATURE)),
                eq(startDate),
                eq(endDate),
                eq("AVG")
        );
    }

    @Test
    @DisplayName("Should calculate max temperature and humidity for multiple sensors")
    void shouldCalculateMaxForMultipleSensorsAndMetrics() {
        // Given
        MetricQueryRequest request = MetricQueryRequest.builder()
                .sensorIds(List.of(1L, 2L, 3L))
                .metricTypes(List.of(MetricType.TEMPERATURE, MetricType.HUMIDITY))
                .statistic(MetricQueryRequest.StatisticType.MAX)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        // Mock repository response (2 metrics)
        List<Object[]> mockResults = new ArrayList<>();
        mockResults.add(new Object[]{MetricType.TEMPERATURE, new BigDecimal("35.20")});
        mockResults.add(new Object[]{MetricType.HUMIDITY, new BigDecimal("85.50")});

        when(metricDataRepository.calculateAggregatedStatistics(
                anyList(), anyList(), any(LocalDateTime.class),
                any(LocalDateTime.class), anyString()))
                .thenReturn(mockResults);

        // When
        List<AggregatedMetricResponse> results =
                metricQueryService.queryAggregatedMetrics(request);

        // Then
        assertThat(results).hasSize(2);

        // Verify temperature result
        AggregatedMetricResponse tempResponse = results.stream()
                .filter(r -> r.getMetricType() == MetricType.TEMPERATURE)
                .findFirst()
                .orElseThrow();
        assertThat(tempResponse.getValue()).isEqualByComparingTo("35.20");
        assertThat(tempResponse.getStatistic()).isEqualTo(MetricQueryRequest.StatisticType.MAX);

        // Verify humidity result
        AggregatedMetricResponse humidityResponse = results.stream()
                .filter(r -> r.getMetricType() == MetricType.HUMIDITY)
                .findFirst()
                .orElseThrow();
        assertThat(humidityResponse.getValue()).isEqualByComparingTo("85.50");
        assertThat(humidityResponse.getStatistic()).isEqualTo(MetricQueryRequest.StatisticType.MAX);

        // Verify repository was called correctly
        verify(metricDataRepository).calculateAggregatedStatistics(
                eq(List.of(1L, 2L, 3L)),
                eq(List.of(MetricType.TEMPERATURE, MetricType.HUMIDITY)),
                eq(startDate),
                eq(endDate),
                eq("MAX")
        );
    }

    @Test
    @DisplayName("Should calculate sum with null sensor IDs (all sensors)")
    void shouldCalculateSumForAllSensors() {
        // Given
        MetricQueryRequest request = MetricQueryRequest.builder()
                .sensorIds(null)  // null = all sensors
                .metricTypes(List.of(MetricType.WIND_SPEED))
                .statistic(MetricQueryRequest.StatisticType.SUM)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        // Mock repository response
        List<Object[]> mockResults = new ArrayList<>();
        mockResults.add(new Object[]{MetricType.WIND_SPEED, new BigDecimal("450.75")});

        when(metricDataRepository.calculateAggregatedStatistics(
                isNull(), anyList(), any(LocalDateTime.class),
                any(LocalDateTime.class), anyString()))
                .thenReturn(mockResults);

        // When
        List<AggregatedMetricResponse> results =
                metricQueryService.queryAggregatedMetrics(request);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getValue()).isEqualByComparingTo("450.75");
        assertThat(results.get(0).getStatistic()).isEqualTo(MetricQueryRequest.StatisticType.SUM);
        assertThat(results.get(0).getSensorIds()).isNull();

        // Verify null was passed for sensor IDs
        verify(metricDataRepository).calculateAggregatedStatistics(
                isNull(),
                eq(List.of(MetricType.WIND_SPEED)),
                eq(startDate),
                eq(endDate),
                eq("SUM")
        );
    }

    @Test
    @DisplayName("Should apply default date range when dates are null")
    void shouldApplyDefaultDateRangeWhenDatesAreNull() {
        // Given
        MetricQueryRequest request = MetricQueryRequest.builder()
                .sensorIds(List.of(1L))
                .metricTypes(List.of(MetricType.TEMPERATURE))
                .statistic(MetricQueryRequest.StatisticType.AVG)
                .startDate(null)  // Will default to 7 days ago
                .endDate(null)    // Will default to now
                .build();

        // Mock repository response
        List<Object[]> mockResults = new ArrayList<>();
        mockResults.add(new Object[]{MetricType.TEMPERATURE, new BigDecimal("22.00")});

        when(metricDataRepository.calculateAggregatedStatistics(
                anyList(), anyList(), any(LocalDateTime.class),
                any(LocalDateTime.class), anyString()))
                .thenReturn(mockResults);

        // When
        List<AggregatedMetricResponse> results =
                metricQueryService.queryAggregatedMetrics(request);

        // Then
        assertThat(results).hasSize(1);

        // Verify default dates were applied (approximately 7 days)
        AggregatedMetricResponse response = results.get(0);
        assertThat(response.getStartDate()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(response.getEndDate()).isBeforeOrEqualTo(LocalDateTime.now());

        long daysDiff = java.time.Duration.between(
                response.getStartDate(),
                response.getEndDate()
        ).toDays();
        assertThat(daysDiff).isBetween(6L, 8L);  // ~7 days

        // Verify repository was called (dates will be auto-generated)
        verify(metricDataRepository).calculateAggregatedStatistics(
                eq(List.of(1L)),
                eq(List.of(MetricType.TEMPERATURE)),
                any(LocalDateTime.class),  // Default start date
                any(LocalDateTime.class),  // Default end date
                eq("AVG")
        );
    }

    @Test
    @DisplayName("Should return empty list when no data found")
    void shouldReturnEmptyListWhenNoDataFound() {
        // Given
        MetricQueryRequest request = MetricQueryRequest.builder()
                .sensorIds(List.of(999L))  // Non-existent sensor
                .metricTypes(List.of(MetricType.TEMPERATURE))
                .statistic(MetricQueryRequest.StatisticType.AVG)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        // Mock empty repository response
        when(metricDataRepository.calculateAggregatedStatistics(
                anyList(), anyList(), any(LocalDateTime.class),
                any(LocalDateTime.class), anyString()))
                .thenReturn(new ArrayList<>());

        // When
        List<AggregatedMetricResponse> results =
                metricQueryService.queryAggregatedMetrics(request);

        // Then
        assertThat(results).isEmpty();

        verify(metricDataRepository).calculateAggregatedStatistics(
                eq(List.of(999L)),
                eq(List.of(MetricType.TEMPERATURE)),
                eq(startDate),
                eq(endDate),
                eq("AVG")
        );
    }

    @Test
    @DisplayName("Should calculate minimum value correctly")
    void shouldCalculateMinimumValue() {
        // Given
        MetricQueryRequest request = MetricQueryRequest.builder()
                .sensorIds(List.of(1L))
                .metricTypes(List.of(MetricType.TEMPERATURE))
                .statistic(MetricQueryRequest.StatisticType.MIN)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        // Mock repository response
        List<Object[]> mockResults = new ArrayList<>();
        mockResults.add(new Object[]{MetricType.TEMPERATURE, new BigDecimal("5.30")});

        when(metricDataRepository.calculateAggregatedStatistics(
                anyList(), anyList(), any(LocalDateTime.class),
                any(LocalDateTime.class), anyString()))
                .thenReturn(mockResults);

        // When
        List<AggregatedMetricResponse> results =
                metricQueryService.queryAggregatedMetrics(request);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getValue()).isEqualByComparingTo("5.30");
        assertThat(results.get(0).getStatistic()).isEqualTo(MetricQueryRequest.StatisticType.MIN);

        verify(metricDataRepository).calculateAggregatedStatistics(
                eq(List.of(1L)),
                eq(List.of(MetricType.TEMPERATURE)),
                eq(startDate),
                eq(endDate),
                eq("MIN")
        );
    }
}