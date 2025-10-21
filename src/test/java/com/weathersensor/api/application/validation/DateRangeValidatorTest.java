package com.weathersensor.api.application.validation;

import com.weathersensor.api.application.dto.request.MetricQueryRequest;
import com.weathersensor.api.domain.model.MetricType;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("DateRangeValidator Unit Tests")
class DateRangeValidatorTest {

    private DateRangeValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder;

    @BeforeEach
    void setUp() {
        validator = new DateRangeValidator();

        // Use lenient() to avoid UnnecessaryStubbingException
        // The mocks are configured but not always used (e.g., in successful validations)
        lenient().when(context.buildConstraintViolationWithTemplate(anyString()))
                .thenReturn(violationBuilder);
        lenient().when(violationBuilder.addPropertyNode(anyString()))
                .thenReturn(nodeBuilder);
        lenient().when(nodeBuilder.addConstraintViolation())
                .thenReturn(context);
    }

    @Test
    @DisplayName("Should accept valid 7-day range")
    void shouldAcceptValidSevenDayRange() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        MetricQueryRequest request = MetricQueryRequest.builder()
                .metricTypes(List.of(MetricType.TEMPERATURE))
                .statistic(MetricQueryRequest.StatisticType.AVG)
                .startDate(now.minusDays(7))
                .endDate(now)
                .build();

        // When
        boolean isValid = validator.isValid(request, context);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should accept minimum valid range (1 day)")
    void shouldAcceptMinimumValidRange() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        MetricQueryRequest request = MetricQueryRequest.builder()
                .metricTypes(List.of(MetricType.TEMPERATURE))
                .statistic(MetricQueryRequest.StatisticType.AVG)
                .startDate(now.minusDays(1))
                .endDate(now)
                .build();

        // When
        boolean isValid = validator.isValid(request, context);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should accept maximum valid range (31 days)")
    void shouldAcceptMaximumValidRange() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        MetricQueryRequest request = MetricQueryRequest.builder()
                .metricTypes(List.of(MetricType.TEMPERATURE))
                .statistic(MetricQueryRequest.StatisticType.AVG)
                .startDate(now.minusDays(31))
                .endDate(now)
                .build();

        // When
        boolean isValid = validator.isValid(request, context);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject range less than 1 day")
    void shouldRejectRangeLessThanOneDay() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        MetricQueryRequest request = MetricQueryRequest.builder()
                .metricTypes(List.of(MetricType.TEMPERATURE))
                .statistic(MetricQueryRequest.StatisticType.AVG)
                .startDate(now.minusHours(12))  // Only 12 hours
                .endDate(now)
                .build();

        // When
        boolean isValid = validator.isValid(request, context);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject range greater than 31 days")
    void shouldRejectRangeGreaterThan31Days() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        MetricQueryRequest request = MetricQueryRequest.builder()
                .metricTypes(List.of(MetricType.TEMPERATURE))
                .statistic(MetricQueryRequest.StatisticType.AVG)
                .startDate(now.minusDays(40))  // 40 days exceeds limit
                .endDate(now)
                .build();

        // When
        boolean isValid = validator.isValid(request, context);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should accept null request")
    void shouldAcceptNullRequest() {
        // When
        boolean isValid = validator.isValid(null, context);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should apply default date range when dates are null")
    void shouldApplyDefaultDateRangeWhenDatesAreNull() {
        // Given
        MetricQueryRequest request = MetricQueryRequest.builder()
                .metricTypes(List.of(MetricType.TEMPERATURE))
                .statistic(MetricQueryRequest.StatisticType.AVG)
                .startDate(null)  // Will default to 7 days ago
                .endDate(null)    // Will default to now
                .build();

        // When
        boolean isValid = validator.isValid(request, context);

        // Then
        assertThat(isValid).isTrue();
    }
}