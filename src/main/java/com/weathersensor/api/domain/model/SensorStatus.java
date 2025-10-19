package com.weathersensor.api.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Operational status of a weather sensor.
 */
@Getter
@RequiredArgsConstructor
public enum SensorStatus {
    ACTIVE("Sensor is operational and collecting data"),
    INACTIVE("Sensor is temporarily disabled"),
    MAINTENANCE("Sensor is undergoing maintenance");

    private final String description;
}