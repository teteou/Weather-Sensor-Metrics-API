package com.weathersensor.api.infrastructure.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting configuration using Bucket4j.
 * Implements token bucket algorithm with two limits:
 * - Sustained rate: requests per minute (e.g., 100/min)
 * - Burst capacity: requests per second (e.g., 20/sec)
 * Strategy: In-memory buckets per IP address.
 */
@Configuration
@Slf4j
@Getter
public class RateLimitConfig {

    @Value("${rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${rate-limit.requests-per-minute:100}")
    private long requestsPerMinute;

    @Value("${rate-limit.burst-capacity:20}")
    private long burstCapacity;

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String clientIp) {
        return cache.computeIfAbsent(clientIp, key -> {
            log.debug("Creating new rate limit bucket for IP: {}", clientIp);
            return createNewBucket();
        });
    }

    private Bucket createNewBucket() {
        Bandwidth burstLimit = Bandwidth.builder()
                .capacity(burstCapacity)
                .refillGreedy(burstCapacity, Duration.ofSeconds(1))
                .build();

        Bandwidth sustainedLimit = Bandwidth.builder()
                .capacity(requestsPerMinute)
                .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                .build();

        return Bucket.builder()
                .addLimit(burstLimit)
                .addLimit(sustainedLimit)
                .build();
    }
}