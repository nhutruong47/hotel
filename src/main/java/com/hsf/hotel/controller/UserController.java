package com.hsf.hotel.controller;

import com.hsf.hotel.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PutMapping("/update")
    public ResponseEntity<String> updateUser(@RequestBody Map<String, String> updates) {
        String username = updates.get("username");
        String newPassword = updates.get("password");
        boolean updated = userService.updateUser(username, newPassword);
        if (updated) {
            return ResponseEntity.ok("User updated successfully.");
        } else {
            return ResponseEntity.badRequest().body("Failed to update user.");
        }
    }
}
