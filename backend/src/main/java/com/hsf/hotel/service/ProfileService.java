package com.hsf.hotel.service;

import com.hsf.hotel.config.ErrorCodes;
import com.hsf.hotel.dto.NotificationEvent;
import com.hsf.hotel.dto.PasswordDTO;
import com.hsf.hotel.dto.ProfileDTO;
import com.hsf.hotel.exception.BusinessRuleException;
import com.hsf.hotel.exception.ResourceNotFoundException;
import com.hsf.hotel.model.User;
import com.hsf.hotel.repository.UserRepository;
import com.hsf.hotel.security.TokenHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Owns the profile / password / account-recovery flows. All public
 * methods throw {@link com.hsf.hotel.exception.ApiException} subclasses
 * so the global handler can map them to clean HTTP responses.
 *
 * <p>Verification and password-reset tokens are stored only as SHA-256
 * hashes; the raw token never leaves the email pipeline.
 */
@Service
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);
    private static final int TOKEN_VALIDITY_HOURS = 2;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationProducer notificationProducer;

    public ProfileService(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          NotificationProducer notificationProducer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationProducer = notificationProducer;
    }

    public Optional<User> getCurrentUser() {
        var ctx = SecurityContextHolder.getContext();
        if (ctx == null) {
            return Optional.empty();
        }
        var auth = ctx.getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = auth.getPrincipal();
        String username = null;
        if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else if (principal instanceof String s) {
            username = s;
        }
        if (username == null || "anonymousUser".equals(username)) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username);
    }

    @Transactional
    public User updateProfile(String username, ProfileDTO dto, String avatarFilename) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        if (dto.getFullName() != null) {
            user.setFullName(dto.getFullName());
        }
        if (dto.getEmail() != null && !dto.getEmail().equalsIgnoreCase(user.getEmail())) {
            String email = dto.getEmail().trim().toLowerCase(Locale.ROOT);
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new BusinessRuleException(ErrorCodes.INVALID_EMAIL, "Email khong hop le");
            }
            Optional<User> existing = userRepository.findByEmail(email);
            if (existing.isPresent() && !existing.get().getId().equals(user.getId())) {
                throw new BusinessRuleException(ErrorCodes.EMAIL_TAKEN, "Email da duoc su dung");
            }
            user.setEmail(email);
            user.setEmailVerified(false);
            String token = TokenHasher.generateToken(24);
            user.setVerificationToken(TokenHasher.hash(token));
            user.setTokenExpiry(LocalDateTime.now().plusHours(TOKEN_VALIDITY_HOURS));
            NotificationEvent event = new NotificationEvent("VERIFICATION", user.getId());
            event.setUserId(user.getId());
            event.setRawToken(token);
            notificationProducer.sendEmailNotification(event);
        }
        if (avatarFilename != null && !avatarFilename.isEmpty()) {
            user.setAvatarFilename(avatarFilename);
        }
        if (dto.getPhone() != null) user.setPhone(dto.getPhone().isBlank() ? null : dto.getPhone().trim());
        if (dto.getDateOfBirth() != null) user.setDateOfBirth(dto.getDateOfBirth().isBlank() ? null : dto.getDateOfBirth().trim());
        if (dto.getGender() != null) user.setGender(dto.getGender().isBlank() ? null : dto.getGender().trim());
        if (dto.getNationality() != null) user.setNationality(dto.getNationality().isBlank() ? null : dto.getNationality().trim());
        if (dto.getEmergencyContactName() != null) user.setEmergencyContactName(dto.getEmergencyContactName().isBlank() ? null : dto.getEmergencyContactName().trim());
        if (dto.getEmergencyContactPhone() != null) user.setEmergencyContactPhone(dto.getEmergencyContactPhone().isBlank() ? null : dto.getEmergencyContactPhone().trim());
        if (dto.getEmergencyContactRelation() != null) user.setEmergencyContactRelation(dto.getEmergencyContactRelation().isBlank() ? null : dto.getEmergencyContactRelation().trim());
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(String username, PasswordDTO.UpdatePassword dto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        if (dto.getNewPassword() == null || dto.getNewPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessRuleException(ErrorCodes.WEAK_PASSWORD,
                    "Mật khẩu mới phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessRuleException(ErrorCodes.PASSWORD_MISMATCH,
                    "Mật khẩu mới và xác nhận không khớp");
        }
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new BusinessRuleException("INVALID_CURRENT_PASSWORD", "Mật khẩu hiện tại không đúng");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * User-enumeration safe: returns silently for missing emails, but still
     * issues a token + email when the address is on file. The raw token is
     * never persisted — only its SHA-256 hash is stored in the user row.
     */
    @Transactional
    public void initiatePasswordReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        String normalised = email.trim().toLowerCase(Locale.ROOT);
        var userOpt = userRepository.findByEmail(normalised);
        if (userOpt.isEmpty()) {
            log.info("Password reset requested for unknown email");
            return;
        }
        User user = userOpt.get();
        String token = TokenHasher.generateToken(32);
        user.setResetToken(TokenHasher.hash(token));
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(TOKEN_VALIDITY_HOURS));
        userRepository.save(user);
        
        NotificationEvent event = new NotificationEvent("PASSWORD_RESET", user.getId());
        event.setUserId(user.getId());
        event.setRawToken(token);
        notificationProducer.sendEmailNotification(event);
    }

    @Transactional
    public User updatePreferences(String username, String preferencesJson) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        user.setPreferencesJson(preferencesJson);
        return userRepository.save(user);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new BusinessRuleException(ErrorCodes.INVALID_TOKEN, "Thiếu token đặt lại mật khẩu");
        }
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessRuleException(ErrorCodes.WEAK_PASSWORD,
                    "Mật khẩu mới phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự");
        }
        String hashed = TokenHasher.hash(token);
        User user = userRepository.findByResetToken(hashed)
                .orElseThrow(() -> new BusinessRuleException(ErrorCodes.INVALID_TOKEN,
                        "Link đặt lại không hợp lệ hoặc đã hết hạn"));
        if (user.getResetTokenExpiry() == null
                || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("EXPIRED_TOKEN", "Token đã hết hạn");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
