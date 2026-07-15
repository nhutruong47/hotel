package com.hsf.hotel.service;

import com.hsf.hotel.config.ErrorCodes;
import com.hsf.hotel.exception.BusinessRuleException;
import com.hsf.hotel.exception.ResourceNotFoundException;
import com.hsf.hotel.model.User;
import com.hsf.hotel.repository.UserRepository;
import com.hsf.hotel.security.TokenHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Domain logic for user accounts: registration, login, email verification,
 * password reset. Throws {@link com.hsf.hotel.exception.ApiException}
 * subclasses so the global exception handler maps failures to a uniform
 * response envelope.
 *
 * <p>Verification tokens are stored only as SHA-256 hashes; the raw token
 * is delivered exclusively by email and never persisted.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final int TOKEN_VALIDITY_HOURS = 24;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MIN_USERNAME_LENGTH = 3;

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       EmailService emailService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(String username, String password, String email, String fullName) {
        validateUsername(username);
        validateEmail(email);
        validatePassword(password);

        if (userRepository.findByUsername(username).isPresent()) {
            throw new BusinessRuleException(ErrorCodes.USERNAME_TAKEN, "Tên đăng nhập đã tồn tại");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessRuleException(ErrorCodes.EMAIL_TAKEN, "Email đã được sử dụng");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole("USER");
        user.setEmailVerified(false);
        String rawToken = TokenHasher.generateToken(24);
        user.setVerificationToken(TokenHasher.hash(rawToken));
        user.setTokenExpiry(LocalDateTime.now().plusHours(TOKEN_VALIDITY_HOURS));

        User saved = userRepository.save(user);
        emailService.sendVerificationEmail(saved, rawToken);
        log.info("User registered: {} (pending verification)", username);
        return saved;
    }

    /**
     * Mark a user's email as verified when the token matches and has not
     * expired. Returns {@code true} on success, {@code false} on bad token.
     */
    @Transactional
    public boolean verifyEmail(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String hashed = TokenHasher.hash(token);
        Optional<User> userOpt = userRepository.findByVerificationToken(hashed);
        if (userOpt.isEmpty()) {
            log.info("Invalid verification token");
            return false;
        }
        User user = userOpt.get();
        if (user.getTokenExpiry() != null && user.getTokenExpiry().isBefore(LocalDateTime.now())) {
            log.info("Expired verification token for user {}", user.getUsername());
            return false;
        }
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setTokenExpiry(null);
        userRepository.save(user);
        log.info("Email verified for user {}", user.getUsername());
        return true;
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account", email));
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BusinessRuleException(ErrorCodes.ALREADY_VERIFIED, "Email đã được xác thực");
        }
        String rawToken = TokenHasher.generateToken(24);
        user.setVerificationToken(TokenHasher.hash(rawToken));
        user.setTokenExpiry(LocalDateTime.now().plusHours(TOKEN_VALIDITY_HOURS));
        userRepository.save(user);
        emailService.sendVerificationEmail(user, rawToken);
        log.info("Verification email re-sent to {}", email);
    }

    /**
     * Verify credentials and return the user. Returns empty when the user
     * does not exist OR the password does not match - never reveal which.
     */
    public Optional<User> login(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty() || !passwordEncoder.matches(password, userOpt.get().getPassword())) {
            return Optional.empty();
        }
        if (Boolean.TRUE.equals(userOpt.get().getDisabled())) {
            log.info("User {} attempted to sign in on a disabled account", username);
            return Optional.empty();
        }
        if (!Boolean.TRUE.equals(userOpt.get().getEmailVerified())) {
            log.info("User {} signed in with unverified email", username);
        }
        return userOpt;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findById(Integer id) {
        return userRepository.findById(id);
    }

    public long countAllUsers() {
        return userRepository.count();
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Admin operation: soft-disable a user account. Disabled accounts cannot
     * log in or use authenticated endpoints but their historical data is
     * preserved.
     */
    @Transactional
    public void disableUser(Integer id, String reason, User actor) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        if ("ADMIN".equals(user.getRole()) && actor != null && actor.getId().equals(user.getId())) {
            throw new BusinessRuleException("SELF_DISABLE_ADMIN", "Admin cannot disable their own account");
        }
        user.setDisabled(true);
        user.setDisabledAt(LocalDateTime.now());
        user.setDisabledReason(reason);
        userRepository.save(user);
        log.info("User {} disabled by {}", user.getUsername(),
                actor != null ? actor.getUsername() : "system");
    }

    @Transactional
    public void enableUser(Integer id, User actor) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setDisabled(false);
        user.setDisabledAt(null);
        user.setDisabledReason(null);
        userRepository.save(user);
        log.info("User {} re-enabled by {}", user.getUsername(),
                actor != null ? actor.getUsername() : "system");
    }

    /** Returns true when the account is administratively disabled. */
    public boolean isDisabled(User user) {
        return user != null && Boolean.TRUE.equals(user.getDisabled());
    }

    @Transactional
    public boolean updateUser(String username, String newPassword) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty() || newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            return false;
        }
        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

    /* ---------- validation helpers ---------- */

    private void validateUsername(String username) {
        if (username == null || username.length() < MIN_USERNAME_LENGTH) {
            throw new BusinessRuleException(ErrorCodes.INVALID_USERNAME,
                    "Tên đăng nhập phải có ít nhất " + MIN_USERNAME_LENGTH + " ký tự");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessRuleException(ErrorCodes.INVALID_EMAIL, "Email là bắt buộc");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessRuleException(ErrorCodes.INVALID_EMAIL, "Email không hợp lệ");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessRuleException(ErrorCodes.WEAK_PASSWORD,
                    "Mật khẩu phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự");
        }
    }
}