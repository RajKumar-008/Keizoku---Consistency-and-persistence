package com.dayforge.controller;

import com.dayforge.dto.NotificationPrefDto;
import com.dayforge.entity.User;
import com.dayforge.service.AuthService;
import com.dayforge.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthService authService;

    public NotificationController(NotificationService notificationService, AuthService authService) {
        this.notificationService = notificationService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getNotifications() {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(notificationService.getLiveNotifications(user));
    }

    @GetMapping("/preferences")
    public ResponseEntity<NotificationPrefDto> getPreferences() {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(notificationService.getPreferences(user));
    }

    @PutMapping("/preferences")
    public ResponseEntity<NotificationPrefDto> updatePreferences(@RequestBody NotificationPrefDto dto) {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(notificationService.updatePreferences(user, dto));
    }
}

