package com.hsf.hotel.controller;

import com.hsf.hotel.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WebController {

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    /**
     * WORKFLOW: Registration với email verification
     * 1. Validate username, email, password
     * 2. Generate verification token
     * 3. Save user (emailVerified = false)
     * 4. Send verification email
     */
    @PostMapping("/register")
    public String registerUser(@RequestParam String username,
            @RequestParam String password,
            @RequestParam String email,
            @RequestParam(required = false) String fullName,
            RedirectAttributes redirectAttributes) {
        try {
            userService.registerUser(username, password, email, fullName);
            redirectAttributes.addFlashAttribute("success",
                    "Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/login?register";
        }
    }

    /**
     * WORKFLOW: Xác thực email qua token
     */
    @GetMapping("/verify")
    public String verifyEmail(@RequestParam String token, RedirectAttributes redirectAttributes) {
        boolean success = userService.verifyEmail(token);

        if (success) {
            redirectAttributes.addFlashAttribute("success",
                    "Email đã được xác thực! Bạn có thể đăng nhập.");
        } else {
            redirectAttributes.addFlashAttribute("error",
                    "Link xác thực không hợp lệ hoặc đã hết hạn.");
        }

        return "redirect:/login";
    }

    /**
     * Resend verification email
     */
    @PostMapping("/resend-verification")
    public String resendVerification(@RequestParam String email, RedirectAttributes redirectAttributes) {
        try {
            userService.resendVerificationEmail(email);
            redirectAttributes.addFlashAttribute("success",
                    "Email xác thực đã được gửi lại. Vui lòng kiểm tra hộp thư.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/login";
    }

    // NOTE: Authentication is handled manually via this POST when Spring Security is disabled.
    @PostMapping("/login")
    public String loginUser(@RequestParam String username,
                            @RequestParam String password,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        return userService.login(username, password)
                .map(user -> {
                    session.setAttribute("user", user);
                    if ("ADMIN".equals(user.getRole())) {
                        return "redirect:/admin";
                    }
                    return "redirect:/";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Sai tên đăng nhập hoặc mật khẩu");
                    return "redirect:/login?error";
                });
    }

    @GetMapping("/aesthetics")
    public String aestheticsPage() {
        return "aesthetics";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
