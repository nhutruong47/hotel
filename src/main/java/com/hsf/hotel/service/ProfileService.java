package com.hsf.hotel.service;

import com.hsf.hotel.dto.PasswordDTO;
import com.hsf.hotel.dto.ProfileDTO;
import com.hsf.hotel.model.User;
import com.hsf.hotel.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    private static final int TOKEN_VALIDITY_HOURS = 2;

    public Optional<User> getCurrentUser() {
        var ctx = SecurityContextHolder.getContext();
        if (ctx == null)
            return Optional.empty();
        var auth = ctx.getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return Optional.empty();
        Object principal = auth.getPrincipal();
        String username = null;
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            username = (String) principal;
        }
        if (username == null || "anonymousUser".equals(username))
            return Optional.empty();
        return userRepository.findByUsername(username);
    }

    @Transactional
    public User updateProfile(String username, ProfileDTO dto, String avatarFilename) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        if (dto.getFullName() != null)
            user.setFullName(dto.getFullName());
        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())) {
            user.setEmail(dto.getEmail());
            user.setEmailVerified(false);
            String token = UUID.randomUUID().toString();
            user.setVerificationToken(token);
            user.setTokenExpiry(LocalDateTime.now().plusHours(TOKEN_VALIDITY_HOURS));
            // send verification email
            emailService.sendVerificationEmail(user);
        }
        // update avatar filename if provided
        if (avatarFilename != null && !avatarFilename.isEmpty()) {
            user.setAvatarFilename(avatarFilename);
        }
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(String username, PasswordDTO dto) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        if (dto.getNewPassword() == null || dto.getNewPassword().length() < 6) {
            throw new RuntimeException("New password must be at least 6 characters");
        }

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("New password and confirm password do not match");
        }

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(TOKEN_VALIDITY_HOURS));
        userRepository.save(user);

        // send reset email (uses dedicated reset template/link)
        emailService.sendPasswordResetEmail(user);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token).orElseThrow(() -> new RuntimeException("Invalid token"));
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    /**
     * Tìm user theo email
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Reset password trực tiếp bằng email (luồng đơn giản, không cần token)
     * Dùng khi user xác nhận đúng email → cho reset password luôn
     */
    @Transactional
    public void resetPasswordByEmail(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Account not found with this email"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        System.out.println("✅ Password reset for: " + email);
    }
}
