package com.dayforge.controller;

import com.dayforge.dto.HabitDto;
import com.dayforge.entity.User;
import com.dayforge.service.AuthService;
import com.dayforge.service.HabitService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/habits")
public class HabitController {

    private final HabitService habitService;
    private final AuthService authService;

    public HabitController(HabitService habitService, AuthService authService) {
        this.habitService = habitService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<HabitDto>> getHabits() {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(habitService.getUserHabits(user));
    }

    @PostMapping
    public ResponseEntity<HabitDto> createHabit(@Valid @RequestBody HabitDto dto) {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(habitService.createHabit(user, dto));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<HabitDto>> createBatchHabits(@RequestBody List<HabitDto> dtos) {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(habitService.createBatchHabits(user, dtos));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HabitDto> updateHabit(
        @PathVariable Long id,
        @Valid @RequestBody HabitDto dto
    ) {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(habitService.updateHabit(user, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteHabit(@PathVariable Long id) {
        User user = authService.getCurrentUser();
        habitService.deleteHabit(user, id);
        return ResponseEntity.ok(Map.of("message", "Habit successfully deleted"));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<HabitDto> toggleHabitActive(@PathVariable Long id) {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(habitService.toggleHabitActive(user, id));
    }
}

