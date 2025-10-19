package com.weathersensor.api.application.validation;

import com.weathersensor.api.application.dto.request.MetricQueryRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class DateRangeValidator implements
        ConstraintValidator<ValidDateRange, MetricQueryRequest> {

    private static final long MIN_DAYS = 1;
    private static final long MAX_DAYS = 31;

    @Override
    public boolean isValid(MetricQueryRequest request,
                           ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        LocalDateTime start = request.getStartDate();
        LocalDateTime end = request.getEndDate();

        if (start == null) {
            start = LocalDateTime.now().minusDays(7);
        }
        if (end == null) {
            end = LocalDateTime.now();
        }

        long daysBetween = ChronoUnit.DAYS.between(start, end);

        if (daysBetween < MIN_DAYS) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            "Date range must be at least 1 day")
                    .addPropertyNode("startDate")
                    .addConstraintViolation();
            return false;
        }

        if (daysBetween > MAX_DAYS) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            "Date range cannot exceed 1 month (31 days)")
                    .addPropertyNode("endDate")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}