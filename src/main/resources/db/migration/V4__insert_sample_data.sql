-- Insert test sensors
INSERT INTO sensors (sensor_code, location, status) VALUES
                                                        ('SENSOR-001', 'Madrid - Centro', 'ACTIVE'),
                                                        ('SENSOR-002', 'Barcelona - Costa', 'ACTIVE'),
                                                        ('SENSOR-003', 'Valencia - Puerto', 'ACTIVE'),
                                                        ('SENSOR-004', 'Sevilla - Norte', 'MAINTENANCE'),
                                                        ('SENSOR-005', 'Bilbao - Industrial', 'ACTIVE');

-- Function to generate realistic temperature data
CREATE OR REPLACE FUNCTION generate_temperature(base_temp NUMERIC, hour INT, randomness NUMERIC)
RETURNS NUMERIC AS $$
BEGIN
    -- Diurnal variation: warmer at noon, colder at night
RETURN base_temp +
       (SIN((hour - 6) * PI() / 12) * 5) +
       (RANDOM() * randomness - randomness/2);
END;
$$ LANGUAGE plpgsql;

-- Function to generate humidity
CREATE OR REPLACE FUNCTION generate_humidity(base_humidity NUMERIC, temperature NUMERIC)
RETURNS NUMERIC AS $$
BEGIN
    -- Humidity inversely proportional to temperature
RETURN LEAST(100, GREATEST(0,
                           base_humidity - ((temperature - 20) * 2) + (RANDOM() * 10 - 5)
                  ));
END;
$$ LANGUAGE plpgsql;

-- Generate data for the last 30 days for each sensor
DO $$
DECLARE
sensor_rec RECORD;
    measurement_timestamp TIMESTAMP;  -- Changed variable name to avoid conflict
    base_temp NUMERIC;
    base_humidity NUMERIC;
    hour_offset INT;
BEGIN
FOR sensor_rec IN SELECT id, sensor_code FROM sensors WHERE status = 'ACTIVE'
    LOOP
        -- Base configuration per sensor
        CASE sensor_rec.sensor_code
            WHEN 'SENSOR-001' THEN
                base_temp := 15; base_humidity := 65;  -- Madrid
WHEN 'SENSOR-002' THEN
                base_temp := 18; base_humidity := 70;  -- Barcelona
WHEN 'SENSOR-003' THEN
                base_temp := 20; base_humidity := 75;  -- Valencia
WHEN 'SENSOR-005' THEN
                base_temp := 14; base_humidity := 80;  -- Bilbao
ELSE
                base_temp := 16; base_humidity := 70;
END CASE;

        -- Generate data every 2 hours for 30 days
FOR day_offset IN 0..29 LOOP
            FOR hour_offset IN 0..23 BY 2 LOOP
                measurement_timestamp := NOW() - INTERVAL '1 day' * day_offset + INTERVAL '1 hour' * hour_offset;

                -- TEMPERATURE
INSERT INTO metric_data (sensor_id, metric_type, value, timestamp)
VALUES (
           sensor_rec.id,
           'TEMPERATURE',
           ROUND(generate_temperature(base_temp, hour_offset, 3)::NUMERIC, 2),
           measurement_timestamp
       );

-- HUMIDITY
INSERT INTO metric_data (sensor_id, metric_type, value, timestamp)
VALUES (
           sensor_rec.id,
           'HUMIDITY',
           ROUND(generate_humidity(base_humidity, base_temp)::NUMERIC, 2),
           measurement_timestamp
       );

-- WIND_SPEED (0-30 km/h with realistic variation)
INSERT INTO metric_data (sensor_id, metric_type, value, timestamp)
VALUES (
           sensor_rec.id,
           'WIND_SPEED',
           ROUND((RANDOM() * 20 + 5)::NUMERIC, 2),
           measurement_timestamp
       );

-- PRESSURE (980-1030 hPa)
INSERT INTO metric_data (sensor_id, metric_type, value, timestamp)
VALUES (
           sensor_rec.id,
           'PRESSURE',
           ROUND((990 + RANDOM() * 10)::NUMERIC, 2),  -- 990-1000 hPa range
           measurement_timestamp
       );
END LOOP;
END LOOP;
END LOOP;
END $$;

-- Cleanup temporary functions
DROP FUNCTION IF EXISTS generate_temperature(NUMERIC, INT, NUMERIC);
DROP FUNCTION IF EXISTS generate_humidity(NUMERIC, NUMERIC);

-- Verify inserted data
DO $$
DECLARE
total_sensors INT;
    total_metrics INT;
    date_range TEXT;
BEGIN
SELECT COUNT(*) INTO total_sensors FROM sensors;
SELECT COUNT(*) INTO total_metrics FROM metric_data;
SELECT
    TO_CHAR(MIN(timestamp), 'YYYY-MM-DD') || ' to ' ||
    TO_CHAR(MAX(timestamp), 'YYYY-MM-DD')
INTO date_range
FROM metric_data;

RAISE NOTICE 'Sample data created successfully:';
    RAISE NOTICE '  - Sensors: %', total_sensors;
    RAISE NOTICE '  - Metric data points: %', total_metrics;
    RAISE NOTICE '  - Date range: %', date_range;
END $$;