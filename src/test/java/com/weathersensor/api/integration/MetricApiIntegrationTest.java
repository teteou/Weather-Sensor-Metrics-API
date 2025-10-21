package com.weathersensor.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weathersensor.api.application.dto.request.MetricDataRequest;
import com.weathersensor.api.application.dto.request.MetricQueryRequest;
import com.weathersensor.api.domain.model.MetricType;
import com.weathersensor.api.domain.model.Sensor;
import com.weathersensor.api.domain.model.SensorStatus;
import com.weathersensor.api.domain.repository.MetricDataRepository;
import com.weathersensor.api.domain.repository.SensorRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Metric API.
 * Uses TestContainers to spin up a real PostgreSQL database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Metric API Integration Tests")
class MetricApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("weather_sensor_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private MetricDataRepository metricDataRepository;

    private Long testSensorId;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        metricDataRepository.deleteAll();
        sensorRepository.deleteAll();

        // Create test sensor
        Sensor testSensor = Sensor.builder()
                .sensorCode("TEST-SENSOR-" + System.currentTimeMillis())  // Unique code
                .location("Test Location")
                .status(SensorStatus.ACTIVE)
                .build();
        testSensor = sensorRepository.save(testSensor);
        testSensorId = testSensor.getId();
    }

    @Test
    @DisplayName("Should successfully ingest metric data")
    @Transactional
    void shouldSuccessfullyIngestMetricData() throws Exception {
        // Given
        MetricDataRequest request = new MetricDataRequest(
                testSensorId,
                MetricType.TEMPERATURE,
                new BigDecimal("23.5"),
                LocalDateTime.now()
        );

        // When & Then
        mockMvc.perform(post("/api/v1/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.sensorId").value(testSensorId))
                .andExpect(jsonPath("$.metricType").value("TEMPERATURE"))
                .andExpect(jsonPath("$.value").value(23.5))
                .andExpect(jsonPath("$.unit").value("°C"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("Should reject metric data for non-existent sensor")
    void shouldRejectMetricDataForNonExistentSensor() throws Exception {
        // Given
        MetricDataRequest request = new MetricDataRequest(
                999L,  // Non-existent sensor
                MetricType.TEMPERATURE,
                new BigDecimal("23.5"),
                LocalDateTime.now()
        );

        // When & Then
        mockMvc.perform(post("/api/v1/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject metric data with invalid value")
    void shouldRejectMetricDataWithInvalidValue() throws Exception {
        // Given - value exceeds max allowed (1000)
        MetricDataRequest request = new MetricDataRequest(
                testSensorId,
                MetricType.TEMPERATURE,
                new BigDecimal("1500.0"),
                LocalDateTime.now()
        );

        // When & Then
        mockMvc.perform(post("/api/v1/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @DisplayName("Should query aggregated metrics successfully")
    void shouldQueryAggregatedMetricsSuccessfully() throws Exception {
        // Given - Ingest multiple data points first
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < 5; i++) {
            MetricDataRequest request = new MetricDataRequest(
                    testSensorId,
                    MetricType.TEMPERATURE,
                    new BigDecimal("20." + i),
                    now.minusDays(i)
            );

            mockMvc.perform(post("/api/v1/metrics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // When - Query aggregated data
        MetricQueryRequest queryRequest = MetricQueryRequest.builder()
                .sensorIds(List.of(testSensorId))
                .metricTypes(List.of(MetricType.TEMPERATURE))
                .statistic(MetricQueryRequest.StatisticType.AVG)
                .startDate(now.minusDays(7))
                .endDate(now)
                .build();

        // Then
        mockMvc.perform(post("/api/v1/metrics/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queryRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].metricType").value("TEMPERATURE"))
                .andExpect(jsonPath("$[0].value").isNumber())
                .andExpect(jsonPath("$[0].statistic").value("AVG"))
                .andExpect(jsonPath("$[0].startDate").exists())
                .andExpect(jsonPath("$[0].endDate").exists());
    }

    @Test
    @Transactional
    @DisplayName("Should query multiple metrics with MAX statistic")
    void shouldQueryMultipleMetricsWithMaxStatistic() throws Exception {
        // Given - Ingest temperature and humidity data
        LocalDateTime now = LocalDateTime.now();

        // Temperature data
        for (int i = 0; i < 3; i++) {
            MetricDataRequest request = new MetricDataRequest(
                    testSensorId,
                    MetricType.TEMPERATURE,
                    new BigDecimal("25." + i),
                    now.minusDays(i)
            );
            mockMvc.perform(post("/api/v1/metrics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Humidity data
        for (int i = 0; i < 3; i++) {
            MetricDataRequest request = new MetricDataRequest(
                    testSensorId,
                    MetricType.HUMIDITY,
                    new BigDecimal("60." + i),
                    now.minusDays(i)
            );
            mockMvc.perform(post("/api/v1/metrics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // When - Query both metrics with MAX
        MetricQueryRequest queryRequest = MetricQueryRequest.builder()
                .sensorIds(List.of(testSensorId))
                .metricTypes(List.of(MetricType.TEMPERATURE, MetricType.HUMIDITY))
                .statistic(MetricQueryRequest.StatisticType.MAX)
                .startDate(now.minusDays(7))
                .endDate(now)
                .build();

        // Then
        mockMvc.perform(post("/api/v1/metrics/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queryRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].metricType", containsInAnyOrder("TEMPERATURE", "HUMIDITY")))
                .andExpect(jsonPath("$[*].statistic", everyItem(is("MAX"))));
    }

    @Test
    @DisplayName("Should reject query with invalid date range (exceeds 31 days)")
    void shouldRejectQueryWithInvalidDateRange() throws Exception {
        // Given - Date range exceeds 31 days
        LocalDateTime now = LocalDateTime.now();
        MetricQueryRequest queryRequest = MetricQueryRequest.builder()
                .sensorIds(List.of(testSensorId))
                .metricTypes(List.of(MetricType.TEMPERATURE))
                .statistic(MetricQueryRequest.StatisticType.AVG)
                .startDate(now.minusDays(40))  // Exceeds 31 days
                .endDate(now)
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/metrics/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queryRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject query with date range less than 1 day")
    void shouldRejectQueryWithDateRangeLessThanOneDay() throws Exception {
        // Given - Date range less than 1 day
        LocalDateTime now = LocalDateTime.now();
        MetricQueryRequest queryRequest = MetricQueryRequest.builder()
                .sensorIds(List.of(testSensorId))
                .metricTypes(List.of(MetricType.TEMPERATURE))
                .statistic(MetricQueryRequest.StatisticType.AVG)
                .startDate(now.minusHours(12))  // Less than 1 day
                .endDate(now)
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/metrics/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queryRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @DisplayName("Should query all sensors when sensorIds is null")
    void shouldQueryAllSensorsWhenSensorIdsIsNull() throws Exception {
        // Given - Create another sensor and ingest data
        Sensor sensor2 = Sensor.builder()
                .sensorCode("TEST-SENSOR-2-" + System.currentTimeMillis())
                .location("Test Location 2")
                .status(SensorStatus.ACTIVE)
                .build();
        sensor2 = sensorRepository.save(sensor2);

        LocalDateTime now = LocalDateTime.now();

        // Data for sensor 1
        MetricDataRequest request1 = new MetricDataRequest(
                testSensorId,
                MetricType.TEMPERATURE,
                new BigDecimal("20.0"),
                now
        );
        mockMvc.perform(post("/api/v1/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Data for sensor 2
        MetricDataRequest request2 = new MetricDataRequest(
                sensor2.getId(),
                MetricType.TEMPERATURE,
                new BigDecimal("25.0"),
                now
        );
        mockMvc.perform(post("/api/v1/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        // When - Query without specifying sensor IDs
        MetricQueryRequest queryRequest = MetricQueryRequest.builder()
                .sensorIds(null)  // Query all sensors
                .metricTypes(List.of(MetricType.TEMPERATURE))
                .statistic(MetricQueryRequest.StatisticType.AVG)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(1))
                .build();

        // Then - Should aggregate data from both sensors
        mockMvc.perform(post("/api/v1/metrics/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queryRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].metricType").value("TEMPERATURE"))
                .andExpect(jsonPath("$[0].statistic").value("AVG"));
    }
}