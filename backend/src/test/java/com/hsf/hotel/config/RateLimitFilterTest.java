package com.hsf.hotel.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private static MockHttpServletResponse call(RateLimitFilter filter, String method, String path, String ip)
            throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest(method, path);
        req.setRemoteAddr(ip);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        return res;
    }

    @Test
    void sensitiveEndpointsHaveTheirOwnStricterBudget() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(100, 3, 60_000);
        for (int i = 0; i < 3; i++) {
            assertThat(call(filter, "POST", "/api/v1/auth/login", "203.0.113.7").getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse blocked = call(filter, "POST", "/api/v1/auth/login", "203.0.113.7");
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
        // Ordinary API reads from the same client are still allowed.
        assertThat(call(filter, "GET", "/api/v1/rooms", "203.0.113.7").getStatus()).isEqualTo(200);
        // Other clients are unaffected.
        assertThat(call(filter, "POST", "/api/v1/auth/login", "198.51.100.9").getStatus()).isEqualTo(200);
    }

    @Test
    void generalBudgetAppliesToAllApiCalls() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(2, 10, 60_000);
        assertThat(call(filter, "GET", "/api/v1/rooms", "203.0.113.8").getStatus()).isEqualTo(200);
        assertThat(call(filter, "GET", "/api/v1/rooms/1", "203.0.113.8").getStatus()).isEqualTo(200);
        assertThat(call(filter, "GET", "/api/v1/blogs", "203.0.113.8").getStatus()).isEqualTo(429);
    }

    @Test
    void staticFilesAndHealthAreNotLimited() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 1, 60_000);
        for (int i = 0; i < 5; i++) {
            assertThat(call(filter, "GET", "/uploads/villa.webp", "203.0.113.9").getStatus()).isEqualTo(200);
            assertThat(call(filter, "GET", "/api/v1/health", "203.0.113.9").getStatus()).isEqualTo(200);
        }
        assertThat(filter.trackedKeys()).isZero();
    }

    @Test
    void sensitivityIsMethodAware() {
        assertThat(RateLimitFilter.isSensitive("POST", "/api/v1/bookings")).isTrue();
        assertThat(RateLimitFilter.isSensitive("GET", "/api/v1/bookings")).isFalse();
    }
}
