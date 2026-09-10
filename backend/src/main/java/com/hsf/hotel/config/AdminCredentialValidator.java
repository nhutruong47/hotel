package com.hsf.hotel.config;

import com.hsf.hotel.user.model.User;
import com.hsf.hotel.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Validates and provisions admin credentials on startup.
 * 
 * <p>In production (prod profile), this ensures:
 * <ul>
 *   <li>Admin credentials are explicitly set via environment variables</li>
 *   <li>Weak/default passwords are rejected</li>
 * </ul>
 * 
 * <p>In development, this creates a default admin account.
 */
@Component
@Profile("!test")
public class AdminCredentialValidator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminCredentialValidator.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;
    private final String activeProfile;

    public AdminCredentialValidator(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.username:}") String adminUsername,
            @Value("${app.admin.password:}") String adminPassword,
            @Value("${spring.profiles.active:default}") String activeProfile) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.activeProfile = activeProfile;
    }

    @Override
    public void run(String... args) {
        boolean isDev = activeProfile == null || activeProfile.contains("dev") || activeProfile.contains("postgres") || "default".equals(activeProfile);
        
        if (!isDev) {
            validateProductionCredentials();
        } else {
            provisionDevAdmin();
        }
    }

    private void validateProductionCredentials() {
        log.info("Production profile detected - validating admin credentials...");

        // Check username is set
        if (adminUsername == null || adminUsername.isBlank()) {
            throw new IllegalStateException(
                    "CRITICAL: Admin username not configured! " +
                    "Set APP_ADMIN_USERNAME environment variable.");
        }

        // Check password is set
        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "CRITICAL: Admin password not configured! " +
                    "Set APP_ADMIN_PASSWORD environment variable.");
        }

        // Check password strength
        if (isWeakPassword(adminPassword)) {
            throw new IllegalStateException(
                    "CRITICAL: Admin password is too weak! " +
                    "Password must be at least 12 characters with uppercase, lowercase, digits, and special characters.");
        }

        log.info("Admin credentials validated successfully.");
        
        // Ensure admin user exists with correct credentials
        ensureAdminUser();
    }

    private boolean isWeakPassword(String password) {
        if (password.length() < 12) return true;
        if (!password.matches(".*[A-Z].*")) return true;  // No uppercase
        if (!password.matches(".*[a-z].*")) return true;  // No lowercase
        if (!password.matches(".*\\d.*")) return true;     // No digit
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) return true;  // No special char
        
        // Check against common weak passwords
        String lower = password.toLowerCase();
        String[] weakPasswords = {
            "password", "password123", "admin", "admin123",
            "changeme", "welcome", "welcome1", "letmein"
        };
        for (String weak : weakPasswords) {
            if (lower.contains(weak)) return true;
        }
        
        return false;
    }

    private void ensureAdminUser() {
        User admin = userRepository.findByUsername(adminUsername).orElse(null);
        
        if (admin == null) {
            // Create admin user
            admin = new User();
            admin.setUsername(adminUsername);
            admin.setEmail("admin@" + extractDomain());
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole("ADMIN");
            admin.setEmailVerified(true);
            userRepository.save(admin);
            log.info("Created admin user: {}", adminUsername);
        } else {
            // Update password to ensure it matches env var
            admin.setPassword(passwordEncoder.encode(adminPassword));
            userRepository.save(admin);
            log.info("Updated admin user password: {}", adminUsername);
        }
    }

    private String extractDomain() {
        try {
            String baseUrl = System.getenv("APP_BASE_URL");
            if (baseUrl != null && baseUrl.contains("//")) {
                return baseUrl.split("//")[1].split("/")[0].replace(":", "-");
            }
        } catch (Exception ignored) {}
        return "hotel.com";
    }

    private void provisionDevAdmin() {
        // In dev mode, create default admin if it doesn't exist
        if (adminUsername == null || adminUsername.isBlank()) {
            log.warn("Admin username not set - using default 'admin'");
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("⚠️  Admin password not set - using insecure default 'admin123'!");
            log.warn("⚠️  DO NOT use this in production!");
        }

        String username = (adminUsername != null && !adminUsername.isBlank()) 
                ? adminUsername : "admin";
        String password = (adminPassword != null && !adminPassword.isBlank()) 
                ? adminPassword : "admin123";

        if (userRepository.findByUsername(username).isEmpty()) {
            User admin = new User();
            admin.setUsername(username);
            admin.setEmail("admin@dev.local");
            admin.setPassword(passwordEncoder.encode(password));
            admin.setRole("ADMIN");
            admin.setEmailVerified(true);
            userRepository.save(admin);
            log.info("Created development admin user: {} / {}", username, 
                    "prod".equals(activeProfile) ? "***" : password);
        }
    }
}
