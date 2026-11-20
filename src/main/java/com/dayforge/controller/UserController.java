package com.dayforge.controller;

import com.dayforge.entity.User;
import com.dayforge.repository.UserRepository;
import com.dayforge.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public UserController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile() {
        User user = authService.getCurrentUser();
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("email", user.getEmail());
        map.put("fullName", user.getFullName());
        map.put("timezone", user.getTimezone());
        map.put("role", user.getRole());
        map.put("createdAt", user.getCreatedAt());
        return ResponseEntity.ok(map);
    }

    @PutMapping("/settings")
    public ResponseEntity<Map<String, Object>> updateSettings(@RequestBody Map<String, String> payload) {
        User user = authService.getCurrentUser();
        if (payload.containsKey("timezone") && !payload.get("timezone").isBlank()) {
            user.setTimezone(payload.get("timezone"));
        }
        if (payload.containsKey("fullName") && !payload.get("fullName").isBlank()) {
            user.setFullName(payload.get("fullName"));
        }
        user = userRepository.save(Objects.requireNonNull(user));

        Map<String, Object> map = new HashMap<>();
        map.put("message", "Settings updated successfully");
        map.put("timezone", user.getTimezone());
        map.put("fullName", user.getFullName());
        return ResponseEntity.ok(map);
    }

    @PostMapping("/reset-data")
    public ResponseEntity<Map<String, String>> resetUserData() {
        User user = authService.getCurrentUser();
        authService.resetUserData(user);
        return ResponseEntity.ok(Map.of("message", "All personal records have been reset successfully"));
    }
}
