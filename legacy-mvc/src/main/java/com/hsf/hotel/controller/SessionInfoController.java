package com.hsf.hotel.controller;

import com.hsf.hotel.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
public class SessionInfoController {

    // Debug endpoint - returns basic session/auth info. Remove in production.
    @GetMapping("/session-info")
    public Map<String, Object> sessionInfo(HttpServletRequest request) {
        Map<String, Object> out = new HashMap<>();
        out.put("sessionId", request.getSession(false) != null ? request.getSession(false).getId() : null);
        Object sessionUser = request.getSession(false) != null ? request.getSession(false).getAttribute("user") : null;
        if (sessionUser instanceof User) {
            User u = (User) sessionUser;
            Map<String, Object> su = new HashMap<>();
            su.put("username", u.getUsername());
            su.put("fullName", u.getFullName());
            su.put("avatarFilename", u.getAvatarFilename());
            out.put("sessionUser", su);
        } else {
            out.put("sessionUser", null);
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            Map<String, Object> a = new HashMap<>();
            a.put("name", auth.getName());
            a.put("authenticated", auth.isAuthenticated());
            a.put("principal", auth.getPrincipal());
            out.put("securityContext", a);
        } else {
            out.put("securityContext", null);
        }
        return out;
    }
}
