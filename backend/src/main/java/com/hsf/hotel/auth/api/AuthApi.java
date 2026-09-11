package com.hsf.hotel.auth.api;

import com.hsf.hotel.config.ApiResponse;
import com.hsf.hotel.config.ErrorCodes;
import com.hsf.hotel.user.dto.UserSummaryDTO;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.security.AuditActions;
import com.hsf.hotel.admin.service.AuditLogService;
import com.hsf.hotel.common.service.LoginAttemptService;
import com.hsf.hotel.profile.service.ProfileService;
import com.hsf.hotel.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@com.hsf.hotel.config.ApiController
@RequestMapping(com.hsf.hotel.config.ApiPaths.V1 + "/auth")
public class AuthApi {

    private final UserService userService;
    private final ProfileService profileService;
    private final LoginAttemptService loginAttemptService;
    private final AuditLogService auditLogService;
    private final ChangeSessionIdAuthenticationStrategy sessionStrategy =
            new ChangeSessionIdAuthenticationStrategy();

    public AuthApi(UserService userService,
                   ProfileService profileService,
                   LoginAttemptService loginAttemptService,
                   AuditLogService auditLogService) {
        this.userService = userService;
        this.profileService = profileService;
        this.loginAttemptService = loginAttemptService;
        this.auditLogService = auditLogService;
    }

    /* ---------- request DTOs (kept as nested public static for stable JSON shape) ---------- */

    public static class LoginRequest {
        @NotBlank public String username;
        @NotBlank public String password;
    }

    public static class RegisterRequest {
        @NotBlank @Size(min = 3) public String username;
        @NotBlank @Size(min = 8) public String password;
        @NotBlank @Email public String email;
        public String fullName;
    }

    public static class ForgotPasswordRequest {
        @NotBlank @Email public String email;
    }

    public static class ResetPasswordRequest {
        public String token;
        @NotBlank @Size(min = 8) public String newPassword;
        @NotBlank public String confirmPassword;
    }

    /* ---------- endpoints ---------- */

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest req,
                                             HttpServletRequest request,
                                             HttpServletResponse response,
                                             HttpSession session) {
        String clientIp = request.getRemoteAddr();
        String key = req.username + "_" + clientIp;
        if (loginAttemptService.isBlocked(key)) {
            auditLogService.log(null, AuditActions.LOGIN_FAILED, "User", null,
                    "login blocked for username=" + req.username, request);
            return ResponseEntity.status(429)
                    .body(ApiResponse.error(ErrorCodes.LOCKED,
                            "Tài khoản bị khóa tạm thời do nhập sai quá nhiều lần. Thử lại sau 15 phút."));
        }

        return userService.login(req.username, req.password)
                .map(user -> {
                    loginAttemptService.loginSucceeded(key);
                    establishSession(user, request, response);
                    auditLogService.log(user, AuditActions.LOGIN_SUCCESS, "User", user.getId(),
                            "username=" + user.getUsername(), request);
                    return ResponseEntity.ok(ApiResponse.ok(publicUser(user)));
                })
                .orElseGet(() -> {
                    loginAttemptService.loginFailed(key);
                    auditLogService.log(null, AuditActions.LOGIN_FAILED, "User", null,
                            "invalid credentials for username=" + req.username, request);
                    return ResponseEntity.status(401)
                            .body(ApiResponse.error(ErrorCodes.INVALID_CREDENTIALS,
                                    "Sai tên đăng nhập hoặc mật khẩu"));
                });
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(@Valid @RequestBody RegisterRequest req,
                                                HttpServletRequest request) {
        User saved = userService.registerUser(req.username, req.password, req.email, req.fullName);
        auditLogService.log(saved, AuditActions.REGISTER, "User", saved.getId(),
                "username=" + saved.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "user", publicUser(saved),
                "message", "Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản."
        )));
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<?>> verifyEmail(@RequestParam String token) {
        boolean ok = userService.verifyEmail(token);
        if (ok) {
            return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Email đã được xác thực")));
        }
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCodes.INVALID_TOKEN,
                        "Link xác thực không hợp lệ hoặc đã hết hạn"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<?>> resendVerification(@RequestParam String email) {
        userService.resendVerificationEmail(email);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Email xác thực đã được gửi lại")));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User current = null;
        if (session != null) {
            Object u = session.getAttribute("user");
            if (u instanceof User user) {
                current = user;
            }
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        if (current != null) {
            auditLogService.log(current, AuditActions.LOGOUT, "User", current.getId(),
                    "user logged out", request);
        }
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Đã đăng xuất")));
    }

    @GetMapping("/session")
    public ResponseEntity<ApiResponse<?>> session(HttpServletRequest request) {
        Map<String, Object> out = new LinkedHashMap<>();
        HttpSession session = request.getSession(false);

        Map<String, Object> user = null;
        if (session != null && session.getAttribute("user") instanceof User u) {
            user = publicUser(u);
        } else {
            // Build the map eagerly because `user` is referenced from a lambda.
            Map<String, Object> computed = profileService.getCurrentUser()
                    .map(u -> publicUser(u))
                    .orElse(null);
            user = computed;
        }
        out.put("user", user);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        out.put("securityAuthenticated", auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getName()));
        return ResponseEntity.ok(ApiResponse.ok(out));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<?>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req,
                                                      HttpServletRequest request) {
        // Always return success to avoid leaking whether the email exists.
        profileService.initiatePasswordReset(req.email);
        auditLogService.log(null, AuditActions.PASSWORD_RESET_REQUEST, "User", null,
                "forgot-password requested for email=" + req.email, request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "message", "If an account exists for that email, a reset link has been sent."
        )));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<?>> resetPassword(@Valid @RequestBody ResetPasswordRequest req,
                                                    HttpServletRequest request) {
        if (!req.newPassword.equals(req.confirmPassword)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ErrorCodes.PASSWORD_MISMATCH, "Mật khẩu xác nhận không khớp"));
        }
        profileService.resetPassword(req.token, req.newPassword);
        auditLogService.log(null, AuditActions.PASSWORD_RESET_COMPLETE, "User", null,
                "password reset completed", request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Đã đặt lại mật khẩu thành công")));
    }

    /* ---------- helpers ---------- */

    private static Map<String, Object> publicUser(User u) {
        return UserSummaryDTO.from(u).asMap();
    }

    private void establishSession(User user, HttpServletRequest request, HttpServletResponse response) {
        // Rotate session id when an unauthenticated visitor becomes signed in.
        sessionStrategy.onAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        user.getUsername(), null, java.util.List.of()),
                request, response);
        HttpSession newSession = request.getSession(true);
        newSession.setAttribute("user", user);
    }
}
