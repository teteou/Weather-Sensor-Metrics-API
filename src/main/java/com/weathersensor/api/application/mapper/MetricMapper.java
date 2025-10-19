package com.weathersensor.api.application.mapper;

import com.weathersensor.api.application.dto.request.MetricDataRequest;
import com.weathersensor.api.application.dto.response.AggregatedMetricResponse;
import com.weathersensor.api.application.dto.response.MetricDataResponse;
import com.weathersensor.api.application.dto.response.SensorResponse;
import com.weathersensor.api.domain.model.MetricData;
import com.weathersensor.api.domain.model.Sensor;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for converting between domain entities and DTOs.
 * MapStruct generates the implementation at compile time.
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface MetricMapper {

    // ===== METRIC DATA MAPPINGS =====

    /**
     * Maps MetricData entity to MetricDataResponse DTO.
     * Flattens the nested Sensor relationship.
     */
    @Mapping(source = "sensor.id", target = "sensorId")
    @Mapping(source = "sensor.sensorCode", target = "sensorCode")
    @Mapping(source = "metricType.unit", target = "unit")
    MetricDataResponse toResponse(MetricData metricData);

    /**
     * Maps a list of MetricData entities to a list of DTOs.
     */
    List<MetricDataResponse> toResponseList(List<MetricData> metricDataList);

    /**
     * Maps MetricDataRequest DTO to MetricData entity (for ingestion).
     * Ignores fields that will be set by the service layer.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sensor", ignore = true)  // Set by service
    @Mapping(target = "createdAt", ignore = true)  // Auto-generated
    MetricData toEntity(MetricDataRequest request);

    /**
     * Maps a list of request DTOs to entities.
     */
    List<MetricData> toEntityList(List<MetricDataRequest> requests);

    // ===== SENSOR MAPPINGS =====

    /**
     * Maps Sensor entity to SensorResponse DTO.
     */
    SensorResponse toSensorResponse(Sensor sensor);

    /**
     * Maps a list of Sensor entities to DTOs.
     */
    List<SensorResponse> toSensorResponseList(List<Sensor> sensors);

    // ===== CUSTOM MAPPINGS =====

    /**
     * Custom mapping for enum to string conversion.
     * MapStruct will use this for MetricType -> String conversions.
     */
    default String metricTypeToString(com.weathersensor.api.domain.model.MetricType metricType) {
        return metricType != null ? metricType.name() : null;
    }
}