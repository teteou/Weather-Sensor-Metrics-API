package com.weathersensor.api.domain.repository;

import com.weathersensor.api.domain.model.Sensor;
import com.weathersensor.api.domain.model.SensorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for accessing Sensor entities.
 */
@Repository
public interface SensorRepository extends JpaRepository<Sensor, Long> {

    /**
     * Find a sensor by its unique code.
     *
     * @param sensorCode the sensor code
     * @return Optional containing the sensor if found
     */
    Optional<Sensor> findBySensorCode(String sensorCode);

    /**
     * Find all sensors with a specific status.
     *
     * @param status the sensor status
     * @return list of sensors with the given status
     */
    List<Sensor> findByStatus(SensorStatus status);

    /**
     * Check if a sensor with the given code exists.
     *
     * @param sensorCode the sensor code
     * @return true if exists, false otherwise
     */
    boolean existsBySensorCode(String sensorCode);

    /**
     * Count sensors by status (ACTIVE, INACTIVE).
     * Used for health checks.
     */
    long countByStatus(SensorStatus status);
}