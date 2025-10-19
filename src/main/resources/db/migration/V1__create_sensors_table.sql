-- Create sensors table
CREATE TABLE sensors (
                         id BIGSERIAL PRIMARY KEY,
                         sensor_code VARCHAR(50) NOT NULL UNIQUE,
                         location VARCHAR(255),
                         status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP
);

-- Index for searches by sensor code
CREATE INDEX idx_sensor_code ON sensors(sensor_code);

-- Index for filtering by status
CREATE INDEX idx_sensor_status ON sensors(status);

-- Comments for documentation
COMMENT ON TABLE sensors IS 'Registry of physical weather sensors';
COMMENT ON COLUMN sensors.sensor_code IS 'Unique identifier code for the sensor (e.g., SENSOR-001)';
COMMENT ON COLUMN sensors.status IS 'Sensor status: ACTIVE, INACTIVE, MAINTENANCE';