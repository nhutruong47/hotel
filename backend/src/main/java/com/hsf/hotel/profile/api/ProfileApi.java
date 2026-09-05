package com.hsf.hotel.profile.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsf.hotel.config.ApiResponse;
import com.hsf.hotel.config.ErrorCodes;
import com.hsf.hotel.common.dto.PasswordDTO;
import com.hsf.hotel.common.dto.PreferencesDTO;
import com.hsf.hotel.profile.dto.ProfileDTO;
import com.hsf.hotel.user.dto.UserProfileDTO;
import com.hsf.hotel.exception.ApiException;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.security.AuditActions;
import com.hsf.hotel.admin.service.AuditLogService;
import com.hsf.hotel.file.service.FileStorageService;
import com.hsf.hotel.profile.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileApi {

    private final ProfileService profileService;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;

    public ProfileApi(ProfileService profileService,
                      FileStorageService fileStorageService,
                      AuditLogService auditLogService) {
        this.profileService = profileService;
        this.fileStorageService = fileStorageService;
        this.auditLogService = auditLogService;
    }

    private Optional<User> resolveCurrentUser(HttpServletRequest request) {
        Optional<User> userOpt = profileService.getCurrentUser();
        if (userOpt.isPresent()) {
            return userOpt;
        }
        Object u = request.getSession(false) != null
                ? request.getSession(false).getAttribute("user") : null;
        return u instanceof User user ? Optional.of(user) : Optional.empty();
    }

    @GetMapping
    public ResponseEntity<ApiResponse> get(HttpServletRequest request) {
        User user = resolveCurrentUser(request)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        ErrorCodes.UNAUTHORIZED, "Chưa đăng nhập"));
        return ResponseEntity.ok(ApiResponse.ok(UserProfileDTO.from(user)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse> update(@Valid @ModelAttribute ProfileDTO profileDTO,
                                              @RequestParam(required = false) MultipartFile avatar,
                                              HttpServletRequest request) {
        User current = resolveCurrentUser(request)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        ErrorCodes.UNAUTHORIZED, "Chưa đăng nhập"));
        String username = current.getUsername();
        String avatarFilename = null;
        String previousAvatar = current.getAvatarFilename();
        if (avatar != null && !avatar.isEmpty()) {
            avatarFilename = fileStorageService.storeFile(avatar);
        }
        User updated = profileService.updateProfile(username, profileDTO, avatarFilename);
        if (request.getSession(false) != null) {
            request.getSession(false).setAttribute("user", updated);
        }
        if (avatarFilename != null && previousAvatar != null && !previousAvatar.isEmpty()
                && !previousAvatar.equals(avatarFilename)) {
            try {
                fileStorageService.delete(previousAvatar);
            } catch (Exception ignored) {
                // best-effort cleanup of stale avatar
            }
        }
        auditLogService.log(updated, AuditActions.PROFILE_UPDATE, "User", updated.getId(),
                "profile updated", request);
        return ResponseEntity.ok(ApiResponse.ok(UserProfileDTO.from(updated)));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse> changePassword(@Valid @RequestBody PasswordDTO.UpdatePassword passwordDTO,
                                                      HttpServletRequest request) {
        User current = resolveCurrentUser(request)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        ErrorCodes.UNAUTHORIZED, "Chưa đăng nhập"));
        if (!passwordDTO.isConfirmed()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.PASSWORD_MISMATCH,
                    "Mật khẩu xác nhận không khớp với mật khẩu mới");
        }
        profileService.changePassword(current.getUsername(), passwordDTO);
        auditLogService.log(current, AuditActions.PASSWORD_CHANGE, "User", current.getId(),
                "password changed", request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Password changed successfully")));
    }

    private static final Logger log = LoggerFactory.getLogger(ProfileApi.class);
    private static final ObjectMapper PREFERENCES_MAPPER = new ObjectMapper();

    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse> getPreferences(HttpServletRequest request) {
        User current = resolveCurrentUser(request)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        ErrorCodes.UNAUTHORIZED, "Chưa đăng nhập"));
        PreferencesDTO dto = new PreferencesDTO();
        String raw = current.getPreferencesJson();
        if (raw != null && !raw.isBlank()) {
            try {
                dto = PREFERENCES_MAPPER.readValue(raw, PreferencesDTO.class);
            } catch (Exception ex) {
                log.warn("Failed to parse preferences for user {}: {}", current.getId(), ex.getMessage());
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @PutMapping("/preferences")
    public ResponseEntity<ApiResponse> updatePreferences(@RequestBody PreferencesDTO dto,
                                                         HttpServletRequest request) {
        User current = resolveCurrentUser(request)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        ErrorCodes.UNAUTHORIZED, "Chưa đăng nhập"));
        if (dto == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST, "Thiếu payload");
        }
        // Whitelist validation so callers can't smuggle arbitrary keys.
        Map<String, Object> cleaned = new LinkedHashMap<>();
        if (dto.getTheme() != null && List.of("light", "dark", "system").contains(dto.getTheme())) {
            cleaned.put("theme", dto.getTheme());
        }
        if (dto.getLanguage() != null && List.of("vi", "en").contains(dto.getLanguage())) {
            cleaned.put("language", dto.getLanguage());
        }
        if (dto.getCurrency() != null && List.of("VND", "USD").contains(dto.getCurrency())) {
            cleaned.put("currency", dto.getCurrency());
        }
        if (dto.getEmailBooking() != null) cleaned.put("emailBooking", dto.getEmailBooking());
        if (dto.getEmailReminders() != null) cleaned.put("emailReminders", dto.getEmailReminders());
        if (dto.getEmailMarketing() != null) cleaned.put("emailMarketing", dto.getEmailMarketing());
        if (dto.getSmsBooking() != null) cleaned.put("smsBooking", dto.getSmsBooking());

        try {
            String json = PREFERENCES_MAPPER.writeValueAsString(cleaned);
            User updated = profileService.updatePreferences(current.getUsername(), json);
            if (request.getSession(false) != null) {
                request.getSession(false).setAttribute("user", updated);
            }
            auditLogService.log(updated, AuditActions.PROFILE_UPDATE, "User", updated.getId(),
                    "preferences updated", request);
            return ResponseEntity.ok(ApiResponse.ok(updated.getPreferencesJson() != null
                    ? PREFERENCES_MAPPER.readValue(updated.getPreferencesJson(), PreferencesDTO.class)
                    : new PreferencesDTO()));
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR,
                    "Không thể lưu preferences");
        }
    }
}