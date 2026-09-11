package com.hsf.hotel.profile.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsf.hotel.notification.service.NotificationProducer;

import com.hsf.hotel.config.ErrorCodes;
import com.hsf.hotel.notification.dto.NotificationEvent;
import com.hsf.hotel.common.dto.PasswordDTO;
import com.hsf.hotel.common.dto.PreferencesDTO;
import com.hsf.hotel.profile.dto.ProfileDTO;
import com.hsf.hotel.exception.BusinessRuleException;
import com.hsf.hotel.exception.ResourceNotFoundException;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.user.repository.UserRepository;
import com.hsf.hotel.user.service.EmailNormalizer;
import com.hsf.hotel.security.TokenHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
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
    private final ObjectMapper objectMapper;

    public ProfileService(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          NotificationProducer notificationProducer,
                          ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationProducer = notificationProducer;
        this.objectMapper = objectMapper;
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
        if (dto.getEmail() != null) {
            String email = EmailNormalizer.normalize(dto.getEmail());
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new BusinessRuleException(ErrorCodes.INVALID_EMAIL, "Email khong hop le");
            }
            if (!Objects.equals(email, EmailNormalizer.normalize(user.getEmail()))) {
                Optional<User> existing = userRepository.findByEmailIgnoreCase(email);
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
        String normalised = EmailNormalizer.normalize(email);
        var userOpt = userRepository.findByEmailIgnoreCase(normalised);
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

    @Transactional(readOnly = true)
    public PreferencesDTO getPreferences(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        return readPreferences(user.getPreferencesJson(), user.getId());
    }

    @Transactional
    public PreferencesDTO updatePreferences(String username, PreferencesDTO patch) {
        validatePreferences(patch);
        User userRef = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        User user = userRepository.findByIdForUpdate(userRef.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", userRef.getId()));
        PreferencesDTO merged = mergePreferences(readPreferences(user.getPreferencesJson(), user.getId()), patch);
        try {
            user.setPreferencesJson(objectMapper.writeValueAsString(merged));
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot serialize user preferences", ex);
        }
        userRepository.save(user);
        return merged;
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
        String normalizedEmail = EmailNormalizer.normalize(email);
        return normalizedEmail == null || normalizedEmail.isBlank()
                ? Optional.empty()
                : userRepository.findByEmailIgnoreCase(normalizedEmail);
    }

    private PreferencesDTO readPreferences(String raw, Integer userId) {
        if (raw == null || raw.isBlank()) {
            return new PreferencesDTO();
        }
        try {
            return objectMapper.readValue(raw, PreferencesDTO.class);
        } catch (Exception ex) {
            log.warn("Failed to parse preferences for user {}: {}", userId, ex.getMessage());
            return new PreferencesDTO();
        }
    }

    private static PreferencesDTO mergePreferences(PreferencesDTO current, PreferencesDTO patch) {
        if (patch.getTheme() != null) current.setTheme(patch.getTheme());
        if (patch.getLanguage() != null) current.setLanguage(patch.getLanguage());
        if (patch.getCurrency() != null) current.setCurrency(patch.getCurrency());
        if (patch.getEmailBooking() != null) current.setEmailBooking(patch.getEmailBooking());
        if (patch.getEmailReminders() != null) current.setEmailReminders(patch.getEmailReminders());
        if (patch.getEmailMarketing() != null) current.setEmailMarketing(patch.getEmailMarketing());
        if (patch.getSmsBooking() != null) current.setSmsBooking(patch.getSmsBooking());
        return current;
    }

    private static void validatePreferences(PreferencesDTO patch) {
        if (patch == null) {
            throw new BusinessRuleException(ErrorCodes.INVALID_PREFERENCES, "Preferences payload is required");
        }
        if (patch.getTheme() != null && !java.util.Set.of("light", "dark", "system").contains(patch.getTheme())) {
            throw new BusinessRuleException(ErrorCodes.INVALID_PREFERENCES, "Invalid theme preference");
        }
        if (patch.getLanguage() != null && !java.util.Set.of("vi", "en").contains(patch.getLanguage())) {
            throw new BusinessRuleException(ErrorCodes.INVALID_PREFERENCES, "Invalid language preference");
        }
        if (patch.getCurrency() != null && !java.util.Set.of("VND", "USD").contains(patch.getCurrency())) {
            throw new BusinessRuleException(ErrorCodes.INVALID_PREFERENCES, "Invalid currency preference");
        }
    }
}
