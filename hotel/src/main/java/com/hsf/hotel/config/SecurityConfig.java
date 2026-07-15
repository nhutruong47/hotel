package com.hsf.hotel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:4173,http://localhost:8080}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        List<String> origins = Stream.of(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        cfg.setAllowedOrigins(origins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Explicit, narrow allowlist of headers instead of wildcard. CSRF
        // header is required for state-changing requests.
        cfg.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Accept", "Origin",
                "X-Request-Id", "X-XSRF-TOKEN"));
        cfg.setExposedHeaders(List.of("Set-Cookie", "X-Request-Id"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    /**
     * Session authentication strategy that rotates the JSESSIONID after a
     * successful login (mitigates session-fixation) and ensures the session
     * is created only when required.
     */
    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        // ChangeSessionIdAuthenticationStrategy rotates the JSESSIONID after a
        // successful login, mitigating session-fixation. It also copies
        // attributes from the old session by default, which is the desired
        // behaviour for the session-bridge auth model.
        return new org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           SessionAuthBridgeFilter sessionAuthBridgeFilter,
                                           SessionAuthenticationStrategy sessionAuthenticationStrategy) throws Exception {
        http
                .addFilterBefore(sessionAuthBridgeFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable()) // CSRF is enforced by CsrfCookieFilter
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                        .contentSecurityPolicy(csp -> {})
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicyHeader(p -> p.policy(
                                "camera=(), microphone=(), geolocation=(), payment=(), usb=()"))
                )
                .authorizeHttpRequests(authorize -> authorize
                        // Actuator probes (used by orchestrators) must be reachable unauthenticated.
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        // Public static resources (images, CSS, JS, uploaded files)
                        .requestMatchers("/images/**", "/assets/**", "/static/**", "/uploads/**", "/*.html", "/*.js", "/*.css", "/*.ico", "/*.png", "/*.jpg", "/*.webp", "/*.woff*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/rooms", "/api/v1/rooms/**", "/api/v1/reviews/room/**", "/api/v1/blogs", "/api/v1/blogs/**", "/api/v1/info/**", "/api/v1/health", "/api/v1/vouchers/validate", "/api/v1/vouchers/preview", "/api/v1/bookings/vouchers/validate", "/api/v1/promotions/validate", "/api/v1/promotions/preview").permitAll()
                        // Public Auth endpoints
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password", "/api/v1/auth/verify", "/api/v1/auth/resend-verification", "/api/v1/auth/session", "/api/v1/auth/logout").permitAll()
                        // Public voucher preview/validate as POST too (used by booking page)
                        .requestMatchers(HttpMethod.POST, "/api/v1/vouchers/validate", "/api/v1/vouchers/preview", "/api/v1/bookings/vouchers/validate").permitAll()
                        // Public contact form
                        .requestMatchers(HttpMethod.POST, "/api/v1/contact").permitAll()
                        // Admin endpoints require ADMIN role (defense-in-depth even if service-layer checks are forgotten)
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // User self-service endpoints must NOT require ADMIN. They
                        // are reachable by any authenticated user but ownership is
                        // re-checked in the service layer.
                        .requestMatchers(HttpMethod.GET, "/api/v1/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/*").hasRole("ADMIN")
                        // All other API endpoints require an authenticated user
                        .requestMatchers("/api/v1/**").authenticated()
                        // SPA fallback for non-API paths
                        .requestMatchers("/**").permitAll()
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionAuthenticationStrategy(sessionAuthenticationStrategy))
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                    String uri = request.getRequestURI();
                    if (uri != null && uri.startsWith("/api/")) {
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write(
                                "{\"data\":null,\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication required\"}}"
                        );
                    } else {
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        response.sendRedirect("/login");
                    }
                }));

        return http.build();
    }
}
