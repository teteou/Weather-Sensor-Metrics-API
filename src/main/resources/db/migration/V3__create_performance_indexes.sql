-- Composite index for most common queries (sensor + type + time)
CREATE INDEX idx_metric_data_composite
    ON metric_data(sensor_id, metric_type, timestamp DESC);

-- Index for queries by date range
CREATE INDEX idx_metric_data_timestamp
    ON metric_data(timestamp DESC);

-- Index for queries by metric type
CREATE INDEX idx_metric_data_type_time
    ON metric_data(metric_type, timestamp DESC);

-- BRIN index for time-series data (very efficient for sequential data)
CREATE INDEX idx_metric_data_timestamp_brin
    ON metric_data USING BRIN (timestamp);

-- Explanatory comments
COMMENT ON INDEX idx_metric_data_composite IS 'Composite index to optimize queries with multiple filters';
COMMENT ON INDEX idx_metric_data_timestamp_brin IS 'BRIN index optimized for scanning large temporal ranges';