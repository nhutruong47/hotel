package com.hsf.hotel.controller;

import com.hsf.hotel.model.User;
import com.hsf.hotel.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class WebController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user) {
        user.setRole("USER");
        userRepository.save(user);
        return "redirect:/login";
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam String username,
            @RequestParam String password,
            HttpSession session) {
        return userRepository.findByUsername(username)
                .filter(u -> u.getPassword().equals(password))
                .map(u -> {
                    session.setAttribute("user", u);
                    return "redirect:/";
                })
                .orElse("redirect:/login?error");
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

}