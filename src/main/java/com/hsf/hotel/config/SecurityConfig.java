package com.hsf.hotel.config;

import com.hsf.hotel.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class SecurityConfig {

        @Autowired
        private UserRepository userRepository;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public UserDetailsService userDetailsService() {
                return username -> {
                        return userRepository.findByUsername(username)
                                        .map(user -> org.springframework.security.core.userdetails.User
                                                        .withUsername(user.getUsername())
                                                        .password(user.getPassword())
                                                        .roles(user.getRole())
                                                        .build())
                                        .orElseThrow(() -> new UsernameNotFoundException(
                                                        "User not found: " + username));
                };
        }

        @Bean
        public AuthenticationSuccessHandler authenticationSuccessHandler() {
                return new AuthenticationSuccessHandler() {
                        @Override
                        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
                                String username = authentication.getName();
                                userRepository.findByUsername(username)
                                                .ifPresent(user -> request.getSession().setAttribute("user", user));
                                // Redirect to home
                                response.sendRedirect(request.getContextPath() + "/");
                        }
                };
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                // Enable CSRF protection (recommended for form-based apps)
                                .csrf(csrf -> csrf
                                                .ignoringRequestMatchers("/api/**", "/ai-recommend/**") // Disable CSRF
                                                                                                        // for API
                                                                                                        // endpoints
                                )

                                // Configure authorization rules
                                .authorizeHttpRequests(authorize -> authorize
                                                // Public pages - no authentication required
                                                .requestMatchers("/", "/login", "/register", "/forgot-password",
                                                                "/reset-password")
                                                .permitAll()
                                                .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**",
                                                                "/webjars/**")
                                                .permitAll()
                                                .requestMatchers("/rooms", "/room/**", "/search").permitAll()
                                                .requestMatchers("/error", "/favicon.ico").permitAll()

                                                // Admin pages - require ADMIN role
                                                .requestMatchers("/admin/**").hasRole("ADMIN")

                                                // All other pages require authentication
                                                .anyRequest().authenticated())

                                // Configure form login
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/login")
                                                .successHandler(authenticationSuccessHandler())
                                                .failureUrl("/login?error=true")
                                                .permitAll())

                                // Configure logout
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?logout=true")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll())

                                // Configure session management
                                .sessionManagement(session -> session
                                                .maximumSessions(1)
                                                .expiredUrl("/login?expired=true"));

                return http.build();
        }
}
