package com.dayforge.controller;

import com.dayforge.dto.AchievementDto;
import com.dayforge.dto.StreakDto;
import com.dayforge.entity.User;
import com.dayforge.service.AuthService;
import com.dayforge.service.StreakService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/streaks")
public class StreakController {

    private final StreakService streakService;
    private final AuthService authService;

    public StreakController(StreakService streakService, AuthService authService) {
        this.streakService = streakService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<StreakDto> getStreak() {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(streakService.getStreakDto(user));
    }

    @PostMapping("/recalculate")
    public ResponseEntity<StreakDto> recalculate() {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(streakService.recalculateStreak(user));
    }

    @GetMapping("/achievements")
    public ResponseEntity<List<AchievementDto>> getAchievements() {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(streakService.getAllAchievements(user));
    }
}

