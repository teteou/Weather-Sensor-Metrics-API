package com.weathersensor.api.application.service;

import com.weathersensor.api.application.dto.request.MetricDataRequest;
import com.weathersensor.api.application.dto.response.MetricDataResponse;
import com.weathersensor.api.application.mapper.MetricMapper;
import com.weathersensor.api.domain.event.MetricIngestedEvent;
import com.weathersensor.api.domain.model.MetricData;
import com.weathersensor.api.domain.model.MetricType;
import com.weathersensor.api.domain.model.Sensor;
import com.weathersensor.api.domain.repository.MetricDataRepository;
import com.weathersensor.api.domain.repository.SensorRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MetricIngestionService Unit Tests")
class MetricIngestionServiceTest {

    @Mock
    private MetricDataRepository metricDataRepository;

    @Mock
    private SensorRepository sensorRepository;

    @Mock
    private MetricMapper metricMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MetricIngestionService metricIngestionService;

    private Sensor testSensor;
    private MetricDataRequest testRequest;
    private MetricData testMetricData;
    private MetricDataResponse testResponse;

    @BeforeEach
    void setUp() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        metricIngestionService = new MetricIngestionService(
                metricDataRepository,
                sensorRepository,
                metricMapper,
                eventPublisher,
                meterRegistry
        );

        testSensor = Sensor.builder()
                .id(1L)
                .sensorCode("TEST-001")
                .location("Test Location")
                .build();

        testRequest = new MetricDataRequest(
                1L,
                MetricType.TEMPERATURE,
                new BigDecimal("23.5"),
                LocalDateTime.now()
        );

        testMetricData = MetricData.builder()
                .id(100L)
                .sensor(testSensor)
                .metricType(MetricType.TEMPERATURE)
                .value(new BigDecimal("23.5"))
                .timestamp(LocalDateTime.now())
                .build();

        testResponse = MetricDataResponse.builder()
                .id(100L)
                .sensorId(1L)
                .metricType(MetricType.TEMPERATURE)
                .value(new BigDecimal("23.5"))
                .build();
    }

    @Test
    @DisplayName("Should ingest metric data synchronously")
    void shouldIngestMetricDataSync() {
        when(sensorRepository.findById(1L)).thenReturn(Optional.of(testSensor));
        when(metricMapper.toEntity(testRequest)).thenReturn(testMetricData);
        when(metricDataRepository.save(any(MetricData.class))).thenReturn(testMetricData);
        when(metricMapper.toResponse(testMetricData)).thenReturn(testResponse);

        MetricDataResponse result = metricIngestionService.ingestMetricData(testRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getSensorId()).isEqualTo(1L);
        assertThat(result.getMetricType()).isEqualTo(MetricType.TEMPERATURE);

        verify(sensorRepository).findById(1L);
        verify(metricDataRepository).save(any(MetricData.class));
        verify(eventPublisher).publishEvent(any(MetricIngestedEvent.class));
    }

    @Test
    @DisplayName("Should throw exception when sensor not found (sync)")
    void shouldThrowExceptionWhenSensorNotFoundSync() {
        when(sensorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> metricIngestionService.ingestMetricData(testRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sensor not found with ID: 1");

        verify(metricDataRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Should ingest metric data asynchronously")
    void shouldIngestMetricDataAsync() throws Exception {
        when(sensorRepository.findById(1L)).thenReturn(Optional.of(testSensor));
        when(metricMapper.toEntity(testRequest)).thenReturn(testMetricData);
        when(metricDataRepository.save(any(MetricData.class))).thenReturn(testMetricData);

        CompletableFuture<MetricData> future = metricIngestionService.ingestMetricAsync(testRequest);

        MetricData result = future.get(2, TimeUnit.SECONDS);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getSensor()).isEqualTo(testSensor);
        assertThat(result.getMetricType()).isEqualTo(MetricType.TEMPERATURE);

        verify(sensorRepository).findById(1L);
        verify(metricDataRepository).save(any(MetricData.class));
        verify(eventPublisher).publishEvent(any(MetricIngestedEvent.class));
    }

    @Test
    @DisplayName("Should handle multiple concurrent async ingestions")
    void shouldHandleConcurrentAsyncIngestions() throws Exception {
        when(sensorRepository.findById(anyLong())).thenReturn(Optional.of(testSensor));
        when(metricMapper.toEntity(any())).thenReturn(testMetricData);
        when(metricDataRepository.save(any(MetricData.class))).thenReturn(testMetricData);

        CompletableFuture<?>[] futures = new CompletableFuture[10];
        for (int i = 0; i < 10; i++) {
            MetricDataRequest request = new MetricDataRequest(
                    1L,
                    MetricType.TEMPERATURE,
                    new BigDecimal("20." + i),
                    LocalDateTime.now()
            );
            futures[i] = metricIngestionService.ingestMetricAsync(request);
        }

        CompletableFuture.allOf(futures).get(5, TimeUnit.SECONDS);

        for (CompletableFuture<?> future : futures) {
            assertThat(future.isDone()).isTrue();
            assertThat(future.isCompletedExceptionally()).isFalse();
        }

        verify(metricDataRepository, times(10)).save(any(MetricData.class));
        verify(eventPublisher, times(10)).publishEvent(any(MetricIngestedEvent.class));
    }

    @Test
    @DisplayName("Should publish domain event when metric ingested (sync)")
    void shouldPublishDomainEventSync() {
        when(sensorRepository.findById(1L)).thenReturn(Optional.of(testSensor));
        when(metricMapper.toEntity(testRequest)).thenReturn(testMetricData);
        when(metricDataRepository.save(any(MetricData.class))).thenReturn(testMetricData);
        when(metricMapper.toResponse(testMetricData)).thenReturn(testResponse);

        ArgumentCaptor<MetricIngestedEvent> eventCaptor = ArgumentCaptor.forClass(MetricIngestedEvent.class);

        metricIngestionService.ingestMetricData(testRequest);

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        MetricIngestedEvent capturedEvent = eventCaptor.getValue();

        assertThat(capturedEvent).isNotNull();
        assertThat(capturedEvent.getMetricData()).isEqualTo(testMetricData);
        assertThat(capturedEvent.getOccurredAt()).isNotNull();
    }

    @Test
    @DisplayName("Should ingest batch of metrics")
    void shouldIngestMetricsBatch() {
        List<MetricDataRequest> requests = List.of(
                new MetricDataRequest(1L, MetricType.TEMPERATURE, new BigDecimal("20"), LocalDateTime.now()),
                new MetricDataRequest(1L, MetricType.HUMIDITY, new BigDecimal("60"), LocalDateTime.now()),
                new MetricDataRequest(1L, MetricType.WIND_SPEED, new BigDecimal("15"), LocalDateTime.now())
        );

        when(sensorRepository.findById(1L)).thenReturn(Optional.of(testSensor));
        when(metricMapper.toEntity(any())).thenReturn(testMetricData);
        when(metricDataRepository.saveAll(anyList())).thenReturn(List.of(testMetricData, testMetricData, testMetricData));
        when(metricMapper.toResponseList(anyList())).thenReturn(List.of(testResponse, testResponse, testResponse));

        List<MetricDataResponse> results = metricIngestionService.ingestMetricDataBatch(requests);

        assertThat(results).hasSize(3);

        verify(sensorRepository, times(3)).findById(1L);
        verify(metricDataRepository).saveAll(anyList());
        verify(eventPublisher, times(3)).publishEvent(any(MetricIngestedEvent.class));
    }

    @Test
    @DisplayName("Should throw exception in batch when sensor not found")
    void shouldThrowExceptionInBatchWhenSensorNotFound() {
        List<MetricDataRequest> requests = List.of(testRequest);
        when(sensorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> metricIngestionService.ingestMetricDataBatch(requests))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sensor not found with ID: 1");

        verify(metricDataRepository, never()).saveAll(anyList());
    }
}