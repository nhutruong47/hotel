package com.hsf.hotel.service;

import com.hsf.hotel.model.User;
import com.hsf.hotel.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final int TOKEN_VALIDITY_HOURS = 24;

    /**
     * Đăng ký user mới với email verification
     * Workflow:
     * 1. Validate username unique
     * 2. Validate email format
     * 3. Validate password strength
     * 4. Generate verification token
     * 5. Save user with emailVerified = false
     * 6. Send verification email
     */
    @Transactional
    public User registerUser(String username, String password, String email, String fullName) {
        // Step 1: Validate username unique
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        // Step 2: Validate email format
        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Email is mandatory");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new RuntimeException("Invalid email format");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email is already in use");
        }

        // Step 3: Validate password strength
        if (password == null || password.length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }

        // Step 4: Generate verification token
        String token = UUID.randomUUID().toString();

        // Step 5: Create and save user
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole("USER");
        user.setEmailVerified(false);
        user.setVerificationToken(token);
        user.setTokenExpiry(LocalDateTime.now().plusHours(TOKEN_VALIDITY_HOURS));

        User savedUser = userRepository.save(user);

        // Step 6: Send verification email
        emailService.sendVerificationEmail(savedUser);

        System.out.println("✅ User registered: " + username + " (pending verification)");
        return savedUser;
    }

    /**
     * Xác thực email qua token
     */
    @Transactional
    public boolean verifyEmail(String token) {
        Optional<User> userOpt = userRepository.findByVerificationToken(token);

        if (userOpt.isEmpty()) {
            System.out.println("❌ Invalid verification token: " + token);
            return false;
        }

        User user = userOpt.get();

        // Check token expiry
        if (user.getTokenExpiry() != null && user.getTokenExpiry().isBefore(LocalDateTime.now())) {
            System.out.println("❌ Token expired for user: " + user.getUsername());
            return false;
        }

        // Verify email
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setTokenExpiry(null);
        userRepository.save(user);

        System.out.println("✅ Email verified for user: " + user.getUsername());
        return true;
    }

    /**
     * Gửi lại email xác thực
     */
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Account not found with this email"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new RuntimeException("Email is already verified");
        }

        // Generate new token
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setTokenExpiry(LocalDateTime.now().plusHours(TOKEN_VALIDITY_HOURS));
        userRepository.save(user);

        emailService.sendVerificationEmail(user);
        System.out.println("✅ Verification email resent to: " + email);
    }

    /**
     * Login với kiểm tra email verified
     */
    public Optional<User> login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();

        // Check password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Optional.empty();
        }

        // Check email verified (optional - có thể cho login nhưng hạn chế tính năng)
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            System.out.println("⚠️ User " + username + " login with unverified email");
            // Vẫn cho login nhưng có thể hạn chế tính năng
        }

        return Optional.of(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public boolean updateUser(String username, String newPassword) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (newPassword != null && newPassword.length() >= 6) {
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }
}
