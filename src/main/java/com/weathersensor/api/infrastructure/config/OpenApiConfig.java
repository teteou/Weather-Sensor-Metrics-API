package com.weathersensor.api.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI weatherSensorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Weather Sensor Metrics API")
                        .description("""
                                REST API for ingesting and querying weather sensor metrics.
                                
                                ## Features
                                - Ingest metric data from multiple sensors
                                - Query metrics with flexible filtering
                                - Statistical aggregations (MIN, MAX, AVG, SUM)
                                - Time-series data analysis
                                
                                ## Supported Metrics
                                - **TEMPERATURE**: Measured in °C
                                - **HUMIDITY**: Measured in %
                                - **WIND_SPEED**: Measured in km/h
                                - **PRESSURE**: Measured in hPa
                                
                                ## Date Range Rules
                                - Minimum range: 1 day
                                - Maximum range: 31 days (1 month)
                                - Default: Last 7 days if not specified
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Weather Sensor API Team")
                                .email("contact@weathersensor.com")
                                .url("https://github.com/yourusername/weather-sensor-api"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development Server"),
                        new Server()
                                .url("https://api.weathersensor.com")
                                .description("Production Server (example)")
                ));
    }
}