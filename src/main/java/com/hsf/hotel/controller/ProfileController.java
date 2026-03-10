package com.hsf.hotel.controller;

import com.hsf.hotel.dto.PasswordDTO;
import com.hsf.hotel.dto.ProfileDTO;
import com.hsf.hotel.model.User;
import com.hsf.hotel.service.FileStorageService;
import com.hsf.hotel.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Controller
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    @Autowired
    private ProfileService profileService;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping({ "/profile", "/user/profile" })
    public String profilePage(Model model, HttpServletRequest request) {
        // First try to resolve the current user from SecurityContext
        Optional<User> userOpt = profileService.getCurrentUser();
        User user = null;
        if (userOpt.isPresent()) {
            user = userOpt.get();
            log.debug("profilePage: resolved user from SecurityContext: {}", user.getUsername());
        } else {
            // Fallback: some flows set the user into the HTTP session ("user") on login
            Object sessionUser = request.getSession(false) != null ? request.getSession(false).getAttribute("user")
                    : null;
            if (sessionUser instanceof User) {
                user = (User) sessionUser;
                log.debug("profilePage: resolved user from session: {}", user.getUsername());
            }
        }
        if (user == null) {
            log.debug("profilePage: no user found, redirecting to login. session id={}",
                    request.getSession(false) != null ? request.getSession(false).getId() : null);
            return "redirect:/login";
        }
        ProfileDTO dto = new ProfileDTO();
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        model.addAttribute("user", user);
        model.addAttribute("profile", dto);
        return "profile";
    }

    // Helper to resolve the current user either from SecurityContext
    // (ProfileService) or from HTTP session
    private Optional<User> resolveCurrentUser(HttpServletRequest request) {
        Optional<User> userOpt = profileService.getCurrentUser();
        if (userOpt.isPresent()) {
            log.debug("resolveCurrentUser: SecurityContext provided user: {}", userOpt.get().getUsername());
            return userOpt;
        }
        Object sessionUser = request.getSession(false) != null ? request.getSession(false).getAttribute("user") : null;
        if (sessionUser instanceof User) {
            log.debug("resolveCurrentUser: session provided user: {}", ((User) sessionUser).getUsername());
            return Optional.of((User) sessionUser);
        }
        log.debug("resolveCurrentUser: no user found. session id={}",
                request.getSession(false) != null ? request.getSession(false).getId() : null);
        return Optional.empty();
    }

    @PostMapping("/profile")
    public String updateProfile(ProfileDTO profileDTO,
            @RequestParam(required = false) MultipartFile avatar,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        Optional<User> userOpt = resolveCurrentUser(request);
        if (userOpt.isEmpty()) {
            log.debug("updateProfile: no current user, redirecting to login. session id={}",
                    request.getSession(false) != null ? request.getSession(false).getId() : null);
            return "redirect:/login";
        }
        String username = userOpt.get().getUsername();
        try {
            // store avatar if provided
            String avatarFilename = null;
            String previousAvatar = null;
            // capture previous avatar filename from the resolved user (before update)
            previousAvatar = userOpt.get().getAvatarFilename();
            if (avatar != null && !avatar.isEmpty()) {
                avatarFilename = fileStorageService.storeFile(avatar);
            }
            // perform profile update and get refreshed user (pass avatar filename if any)
            User updatedUser = profileService.updateProfile(username, profileDTO, avatarFilename);
            log.debug("updateProfile: updated user {}", username);
            // update session-stored user so UI reflects changes and user stays logged in
            if (request.getSession(false) != null) {
                request.getSession(false).setAttribute("user", updatedUser);
                log.debug("updateProfile: session user refreshed, session id={}", request.getSession(false).getId());
            }
            // delete the previous avatar file if a new one was uploaded and previous exists
            if (avatarFilename != null && previousAvatar != null && !previousAvatar.isEmpty()
                    && !previousAvatar.equals(avatarFilename)) {
                try {
                    fileStorageService.delete(previousAvatar);
                    log.debug("updateProfile: deleted previous avatar {}", previousAvatar);
                } catch (Exception ex) {
                    log.warn("Failed to delete previous avatar {}: {}", previousAvatar, ex.getMessage());
                }
            }
            if (avatarFilename != null) {
                redirectAttributes.addFlashAttribute("success", "Profile updated. Avatar stored: " + avatarFilename);
            } else {
                redirectAttributes.addFlashAttribute("success", "Profile updated.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    @GetMapping("/change-password")
    public String changePasswordPage(Model model, HttpServletRequest request) {
        Optional<User> userOpt = resolveCurrentUser(request);
        if (userOpt.isEmpty()) {
            log.debug("changePasswordPage: no current user, redirecting to login. session id={}",
                    request.getSession(false) != null ? request.getSession(false).getId() : null);
            return "redirect:/login";
        }
        model.addAttribute("passwordDTO", new PasswordDTO());
        return "change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(PasswordDTO passwordDTO, RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        Optional<User> userOpt = resolveCurrentUser(request);
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }
        String username = userOpt.get().getUsername();
        try {
            profileService.changePassword(username, passwordDTO);
            redirectAttributes.addFlashAttribute("success", "Password changed successfully.");
            return "redirect:/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/change-password";
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email, RedirectAttributes redirectAttributes) {
        try {
            // Kiểm tra email có tồn tại không
            Optional<User> userOpt = profileService.findByEmail(email);
            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Email not found in the system.");
                return "redirect:/forgot-password";
            }
            // Email đúng → chuyển thẳng đến form reset password
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("success", "Email verified. Please enter a new password.");
            return "redirect:/reset-password";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(required = false) String token,
            @RequestParam(required = false) String email,
            Model model,
            RedirectAttributes redirectAttributes) {
        // Ưu tiên email (luồng mới không cần token)
        if (email != null && !email.isEmpty()) {
            model.addAttribute("email", email);
            return "reset-password";
        }
        // Fallback: check flash attribute
        if (model.containsAttribute("email")) {
            return "reset-password";
        }
        // Legacy: token-based (vẫn hỗ trợ nếu có)
        if (token != null && !token.isEmpty()) {
            model.addAttribute("token", token);
            return "reset-password";
        }
        redirectAttributes.addFlashAttribute("error", "Please enter your email first.");
        return "redirect:/forgot-password";
    }

    @PostMapping("/reset-password")
    public String resetPasswordSubmit(@RequestParam(required = false) String token,
            @RequestParam(required = false) String email,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {
        try {
            if (newPassword == null || newPassword.length() < 6) {
                throw new RuntimeException("Password must be at least 6 characters");
            }
            if (!newPassword.equals(confirmPassword)) {
                throw new RuntimeException("Passwords do not match");
            }

            // Reset bằng email (luồng mới - đơn giản)
            if (email != null && !email.isEmpty()) {
                profileService.resetPasswordByEmail(email, newPassword);
                redirectAttributes.addFlashAttribute("success",
                        "Password reset successful. You can log in with your new password.");
                return "redirect:/login";
            }

            // Legacy: reset bằng token
            if (token != null && !token.isEmpty()) {
                profileService.resetPassword(token, newPassword);
                redirectAttributes.addFlashAttribute("success",
                        "Password reset successful. You can log in with your new password.");
                return "redirect:/login";
            }

            throw new RuntimeException("Missing email or token info");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            if (email != null && !email.isEmpty()) {
                return "redirect:/reset-password?email=" + email;
            }
            return "redirect:/reset-password?token=" + token;
        }
    }
}
