package com.hsf.hotel.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for P0-01: the XSRF-TOKEN cookie must be readable by the SPA
 * (double-submit pattern). RFC 6265 §5.2.6 treats any "HttpOnly" attribute —
 * including "HttpOnly=false" — as HttpOnly, which hid the token from
 * document.cookie and broke every state-changing browser request.
 */
class CsrfCookieFilterTest {

    private final CsrfCookieFilter filter = new CsrfCookieFilter();

    @Test
    void issuedCookieIsNotHttpOnlySoTheSpaCanReadIt() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).startsWith(CsrfCookieFilter.COOKIE_NAME + "=");
        assertThat(setCookie.toLowerCase()).doesNotContain("httponly");
        assertThat(setCookie).contains("SameSite=Strict");
    }

    @Test
    void secureRequestsGetSecureCookie() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("Set-Cookie")).contains("; Secure");
    }

    @Test
    void stateChangingRequestWithMatchingHeaderPasses() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/bookings");
        request.setCookies(new Cookie(CsrfCookieFilter.COOKIE_NAME, "token-123"));
        request.addHeader(CsrfCookieFilter.HEADER_NAME, "token-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void stateChangingRequestWithoutHeaderIsRejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/bookings");
        request.setCookies(new Cookie(CsrfCookieFilter.COOKIE_NAME, "token-123"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("CSRF_TOKEN_INVALID");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void stateChangingRequestWithWrongHeaderIsRejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/wishlist/1");
        request.setCookies(new Cookie(CsrfCookieFilter.COOKIE_NAME, "token-123"));
        request.addHeader(CsrfCookieFilter.HEADER_NAME, "token-999");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
    }
}
