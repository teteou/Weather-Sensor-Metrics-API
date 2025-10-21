package com.weathersensor.api.infrastructure;

import com.weathersensor.api.infrastructure.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that applies rate limiting to incoming requests.
 * Uses token bucket algorithm via Bucket4j to limit requests per IP address.
 * Returns 429 Too Many Requests when limit is exceeded.
 * Headers added to response:
 * X-RateLimit-Limit: Maximum requests allowed
 * X-RateLimit-Remaining: Tokens remaining
 * X-RateLimit-Retry-After-Seconds: Wait time if blocked (only on 429)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitConfig rateLimitConfig;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {

        if (!rateLimitConfig.isEnabled()) {
            log.debug("Rate limiting is disabled, allowing request");
            return true;
        }

        String clientIp = getClientIp(request);
        log.debug("Rate limiting check for IP: {}", clientIp);

        Bucket bucket = rateLimitConfig.resolveBucket(clientIp);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            addRateLimitHeaders(response, probe);
            log.debug("Request allowed for IP: {} (remaining tokens: {})",
                    clientIp, probe.getRemainingTokens());
            return true;
        } else {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;

            log.warn("Rate limit exceeded for IP: {} (retry after {} seconds)",
                    clientIp, waitForRefill);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.addHeader("X-RateLimit-Limit",
                    String.valueOf(rateLimitConfig.getRequestsPerMinute()));
            response.addHeader("X-RateLimit-Remaining", "0");
            response.addHeader("X-RateLimit-Retry-After-Seconds",
                    String.valueOf(waitForRefill));

            String errorJson = String.format(
                    "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Try again in %d seconds.\",\"status\":429}",
                    waitForRefill
            );
            response.getWriter().write(errorJson);

            return false;
        }
    }

    private void addRateLimitHeaders(HttpServletResponse response, ConsumptionProbe probe) {
        response.addHeader("X-RateLimit-Limit",
                String.valueOf(rateLimitConfig.getRequestsPerMinute()));
        response.addHeader("X-RateLimit-Remaining",
                String.valueOf(probe.getRemainingTokens()));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }
}