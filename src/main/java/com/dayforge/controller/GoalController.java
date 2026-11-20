package com.dayforge.controller;

import com.dayforge.dto.GoalDto;
import com.dayforge.entity.User;
import com.dayforge.service.AuthService;
import com.dayforge.service.GoalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;
    private final AuthService authService;

    public GoalController(GoalService goalService, AuthService authService) {
        this.goalService = goalService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<GoalDto>> getGoals() {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(goalService.getUserGoals(user));
    }

    @PostMapping
    public ResponseEntity<GoalDto> createGoal(@Valid @RequestBody GoalDto dto) {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(goalService.createGoal(user, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteGoal(@PathVariable Long id) {
        User user = authService.getCurrentUser();
        goalService.deleteGoal(user, id);
        return ResponseEntity.ok(Map.of("message", "Goal successfully deleted"));
    }
}

