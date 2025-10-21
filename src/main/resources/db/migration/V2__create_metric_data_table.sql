-- Create metric_data table (time-series data)
CREATE TABLE metric_data (
                             id BIGSERIAL PRIMARY KEY,
                             sensor_id BIGINT NOT NULL,
                             metric_type VARCHAR(50) NOT NULL,
                             value NUMERIC(10, 2) NOT NULL,
                             timestamp TIMESTAMP NOT NULL,
                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             CONSTRAINT fk_sensor
                                 FOREIGN KEY (sensor_id)
                                     REFERENCES sensors(id)
                                     ON DELETE CASCADE,

                             CONSTRAINT chk_metric_type
                                 CHECK (metric_type IN ('TEMPERATURE', 'HUMIDITY', 'WIND_SPEED', 'PRESSURE')),

                             CONSTRAINT chk_value_range
                                 CHECK (value >= -100 AND value <= 1000)
);

-- Comments for documentation
COMMENT ON TABLE metric_data IS 'Time-series data of weather metrics';
COMMENT ON COLUMN metric_data.metric_type IS 'Type of measured metric';
COMMENT ON COLUMN metric_data.value IS 'Numeric value of the measurement';
COMMENT ON COLUMN metric_data.timestamp IS 'Exact moment of the measurement';