package com.hsf.hotel.api;

import com.hsf.hotel.config.ApiResponse;
import com.hsf.hotel.config.ErrorCodes;
import com.hsf.hotel.dto.PasswordDTO;
import com.hsf.hotel.dto.UserSummaryDTO;
import com.hsf.hotel.exception.ApiException;
import com.hsf.hotel.exception.BusinessRuleException;
import com.hsf.hotel.exception.ResourceNotFoundException;
import com.hsf.hotel.model.User;
import com.hsf.hotel.repository.UserRepository;
import com.hsf.hotel.security.AuditActions;
import com.hsf.hotel.service.AuditLogService;
import com.hsf.hotel.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserApi {

    private final UserService userService;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public UserApi(UserService userService,
                   UserRepository userRepository,
                   AuditLogService auditLogService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    private void requireAdmin(HttpSession session) {
        Object u = session.getAttribute("user");
        if (!(u instanceof User user) || !"ADMIN".equals(user.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN, "Admin only");
        }
    }

    private static User requireUser(HttpSession session) {
        Object u = session.getAttribute("user");
        if (!(u instanceof User user)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED, "Chưa đăng nhập");
        }
        return user;
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse> updatePassword(@Valid @RequestBody PasswordDTO.UpdatePassword req,
                                                      HttpSession session,
                                                      HttpServletRequest request) {
        User user = requireUser(session);
        boolean ok = req.isConfirmed()
                && userService.updateUser(user.getUsername(), req.getCurrentPassword(),
                req.getNewPassword(), req.getConfirmPassword());
        if (!ok) {
            auditLogService.log(user, AuditActions.PASSWORD_CHANGE, "User", user.getId(),
                    "password update failed", request);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ErrorCodes.UPDATE_FAILED, "Failed to update user."));
        }
        auditLogService.log(user, AuditActions.PASSWORD_CHANGE, "User", user.getId(),
                "password updated", request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "User updated successfully.")));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> list(HttpSession session) {
        requireAdmin(session);
        List<UserSummaryDTO> users = userRepository.findAll().stream()
                .map(UserSummaryDTO::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> get(@PathVariable Integer id, HttpSession session) {
        requireAdmin(session);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return ResponseEntity.ok(ApiResponse.ok(UserSummaryDTO.from(user)));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<ApiResponse> updateRole(@PathVariable Integer id,
                                                 @Valid @RequestBody PasswordDTO.UpdateRole body,
                                                 HttpSession session,
                                                 HttpServletRequest request) {
        User admin = requireUser(session);
        requireAdmin(session);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        String previousRole = user.getRole();
        user.setRole(body.getRole());
        userRepository.save(user);
        auditLogService.log(admin, AuditActions.ADMIN_USER_ROLE_CHANGE, "User", user.getId(),
                "role " + previousRole + " -> " + body.getRole(), request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Cập nhật quyền thành công")));
    }

    public static class DisableUserRequest {
        public Boolean disabled;
        public String reason;
    }

    @PatchMapping("/{id}/disabled")
    public ResponseEntity<ApiResponse> setDisabled(@PathVariable Integer id,
                                                   @RequestBody DisableUserRequest body,
                                                   HttpSession session,
                                                   HttpServletRequest request) {
        User admin = requireUser(session);
        requireAdmin(session);
        if (body == null || body.disabled == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST,
                    "Thiếu trường 'disabled'");
        }
        if (Boolean.FALSE.equals(body.disabled)) {
            userService.enableUser(id, admin);
            auditLogService.log(admin, AuditActions.ADMIN_USER_DISABLE, "User", id,
                    "admin re-enabled user", request);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Đã mở khóa tài khoản")));
        }
        userService.disableUser(id, body.reason, admin);
        auditLogService.log(admin, AuditActions.ADMIN_USER_DISABLE, "User", id,
                "admin disabled user" + (body.reason != null ? " reason=" + body.reason : ""), request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Đã vô hiệu hóa tài khoản")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Integer id,
                                              HttpSession session,
                                              HttpServletRequest request) {
        User currentUser = requireUser(session);
        requireAdmin(session);
        if (currentUser.getId().equals(id)) {
            throw new BusinessRuleException("SELF_DELETE", "Không thể xóa chính mình!");
        }
        userService.disableUser(id, "Disabled by admin delete action", currentUser);
        auditLogService.log(currentUser, AuditActions.ADMIN_USER_DELETE, "User", id,
                "user disabled by delete action", request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Đã vô hiệu hóa user")));
    }
}
