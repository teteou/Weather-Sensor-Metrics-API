package com.weathersensor.api.infrastructure;

import com.weathersensor.api.infrastructure.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitInterceptor Unit Tests")
class RateLimitInterceptorTest {

    @Mock
    private RateLimitConfig rateLimitConfig;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Bucket bucket;

    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitInterceptor(rateLimitConfig);
    }

    @Test
    @DisplayName("Should allow request when rate limit not exceeded")
    void shouldAllowRequestWhenWithinLimit() throws Exception {
        when(rateLimitConfig.isEnabled()).thenReturn(true);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(rateLimitConfig.resolveBucket(anyString())).thenReturn(bucket);

        var probe = mock(io.github.bucket4j.ConsumptionProbe.class);
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
        when(probe.isConsumed()).thenReturn(true);
        when(probe.getRemainingTokens()).thenReturn(99L);
        when(rateLimitConfig.getRequestsPerMinute()).thenReturn(100L);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(response).addHeader("X-RateLimit-Limit", "100");
        verify(response).addHeader("X-RateLimit-Remaining", "99");
    }

    @Test
    @DisplayName("Should block request when rate limit exceeded")
    void shouldBlockRequestWhenLimitExceeded() throws Exception {
        when(rateLimitConfig.isEnabled()).thenReturn(true);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(rateLimitConfig.resolveBucket(anyString())).thenReturn(bucket);

        var probe = mock(io.github.bucket4j.ConsumptionProbe.class);
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
        when(probe.isConsumed()).thenReturn(false);
        when(probe.getNanosToWaitForRefill()).thenReturn(60_000_000_000L);
        when(rateLimitConfig.getRequestsPerMinute()).thenReturn(100L);

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response).addHeader("X-RateLimit-Retry-After-Seconds", "60");
    }

    @Test
    @DisplayName("Should allow request when rate limiting is disabled")
    void shouldAllowRequestWhenDisabled() throws Exception {
        when(rateLimitConfig.isEnabled()).thenReturn(false);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(rateLimitConfig, never()).resolveBucket(anyString());
    }
}