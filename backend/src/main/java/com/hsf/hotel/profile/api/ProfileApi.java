package com.hsf.hotel.profile.api;

import com.hsf.hotel.admin.service.AuditLogService;
import com.hsf.hotel.common.dto.PasswordDTO;
import com.hsf.hotel.common.dto.PreferencesDTO;
import com.hsf.hotel.config.ApiResponse;
import com.hsf.hotel.config.ErrorCodes;
import com.hsf.hotel.exception.ApiException;
import com.hsf.hotel.file.service.FileStorageService;
import com.hsf.hotel.profile.dto.ProfileDTO;
import com.hsf.hotel.profile.service.ProfileService;
import com.hsf.hotel.security.AuditActions;
import com.hsf.hotel.user.dto.UserProfileDTO;
import com.hsf.hotel.user.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;

@com.hsf.hotel.config.ApiController
@RequestMapping(com.hsf.hotel.config.ApiPaths.V1 + "/profile")
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

    @GetMapping
    public ResponseEntity<ApiResponse<?>> get(HttpServletRequest request) {
        User user = requireCurrentUser(request);
        return ResponseEntity.ok(ApiResponse.ok(UserProfileDTO.from(user)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<?>> update(@Valid @ModelAttribute ProfileDTO profileDTO,
                                                  @RequestParam(required = false) MultipartFile avatar,
                                                  HttpServletRequest request) {
        User current = requireCurrentUser(request);
        String avatarFilename = null;
        String previousAvatar = current.getAvatarFilename();
        if (avatar != null && !avatar.isEmpty()) {
            avatarFilename = fileStorageService.storeFile(avatar);
        }

        User updated = profileService.updateProfile(current.getUsername(), profileDTO, avatarFilename);
        if (request.getSession(false) != null) {
            request.getSession(false).setAttribute("user", updated);
        }
        if (avatarFilename != null && previousAvatar != null && !previousAvatar.isEmpty()
                && !previousAvatar.equals(avatarFilename)) {
            fileStorageService.delete(previousAvatar);
        }
        auditLogService.log(updated, AuditActions.PROFILE_UPDATE, "User", updated.getId(),
                "profile updated", request);
        return ResponseEntity.ok(ApiResponse.ok(UserProfileDTO.from(updated)));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<?>> changePassword(
            @Valid @RequestBody PasswordDTO.UpdatePassword passwordDTO,
            HttpServletRequest request) {
        User current = requireCurrentUser(request);
        if (!passwordDTO.isConfirmed()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.PASSWORD_MISMATCH,
                    "Password confirmation does not match");
        }
        profileService.changePassword(current.getUsername(), passwordDTO);
        auditLogService.log(current, AuditActions.PASSWORD_CHANGE, "User", current.getId(),
                "password changed", request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Password changed successfully")));
    }

    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<?>> getPreferences(HttpServletRequest request) {
        User current = requireCurrentUser(request);
        return ResponseEntity.ok(ApiResponse.ok(profileService.getPreferences(current.getUsername())));
    }

    @RequestMapping(value = "/preferences", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<ApiResponse<?>> updatePreferences(
            @Valid @RequestBody PreferencesDTO patch,
            HttpServletRequest request) {
        User current = requireCurrentUser(request);
        PreferencesDTO updated = profileService.updatePreferences(current.getUsername(), patch);
        auditLogService.log(current, AuditActions.PROFILE_UPDATE, "User", current.getId(),
                "preferences updated", request);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    private User requireCurrentUser(HttpServletRequest request) {
        return resolveCurrentUser(request)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        ErrorCodes.UNAUTHORIZED, "Authentication required"));
    }

    private Optional<User> resolveCurrentUser(HttpServletRequest request) {
        Optional<User> user = profileService.getCurrentUser();
        if (user.isPresent()) {
            return user;
        }
        Object sessionUser = request.getSession(false) != null
                ? request.getSession(false).getAttribute("user")
                : null;
        return sessionUser instanceof User value ? Optional.of(value) : Optional.empty();
    }
}
