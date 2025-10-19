package com.weathersensor.api.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Types of weather metrics that can be measured by sensors.
 */
@Getter
@RequiredArgsConstructor
public enum MetricType {
    TEMPERATURE("°C", "Temperature"),
    HUMIDITY("%", "Humidity"),
    WIND_SPEED("km/h", "Wind Speed"),
    PRESSURE("hPa", "Atmospheric Pressure");

    private final String unit;
    private final String displayName;
}