package com.weathersensor.api.application.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
@Documented
public @interface ValidDateRange {
    String message() default "Date range must be between 1 day and 1 month";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}