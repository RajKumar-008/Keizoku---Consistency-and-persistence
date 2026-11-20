package com.dayforge.controller;

import com.dayforge.dto.AuthRequest;
import com.dayforge.dto.AuthResponse;
import com.dayforge.dto.RegisterRequest;
import com.dayforge.entity.User;
import com.dayforge.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser() {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(new AuthResponse(
            null,
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFullName(),
            user.getTimezone(),
            user.getRole()
        ));
    }
}

