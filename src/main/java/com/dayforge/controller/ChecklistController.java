package com.dayforge.controller;

import com.dayforge.dto.*;
import com.dayforge.entity.User;
import com.dayforge.service.AuthService;
import com.dayforge.service.ChecklistService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/checklist")
public class ChecklistController {

    private final ChecklistService checklistService;
    private final AuthService authService;

    public ChecklistController(ChecklistService checklistService, AuthService authService) {
        this.checklistService = checklistService;
        this.authService = authService;
    }

    @GetMapping("/today")
    public ResponseEntity<DailyChecklistResponse> getTodayChecklist() {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(checklistService.getTodayChecklist(user));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<DailyChecklistResponse> getChecklistForDate(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(checklistService.getDailyChecklist(user, date));
    }

    @PostMapping("/batch")
    public ResponseEntity<DailyChecklistResponse> createBatchItems(
        @Valid @RequestBody BatchCreateItemsDto dto
    ) {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(checklistService.createBatchItems(user, dto));
    }

    @PostMapping("/item")
    public ResponseEntity<DailyChecklistResponse> createSingleItem(
        @Valid @RequestBody CreateItemDto dto
    ) {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(checklistService.createSingleItem(user, dto));
    }

    @PatchMapping("/item/{id}/toggle")
    public ResponseEntity<ChecklistItemDto> toggleItemStatus(@PathVariable Long id) {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(checklistService.toggleItemStatus(user, id));
    }

    @PutMapping("/item/{id}")
    public ResponseEntity<ChecklistItemDto> updateItem(
        @PathVariable Long id,
        @Valid @RequestBody UpdateItemDto dto
    ) {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(checklistService.updateItem(user, id, dto));
    }

    @DeleteMapping("/item/{id}")
    public ResponseEntity<Map<String, String>> deleteItem(@PathVariable Long id) {
        User user = authService.getCurrentUser();
        checklistService.deleteItem(user, id);
        return ResponseEntity.ok(Map.of("message", "Activity successfully deleted"));
    }

    @PostMapping("/item/{id}/reorder")
    public ResponseEntity<DailyChecklistResponse> reorderItem(
        @PathVariable Long id,
        @RequestBody Map<String, Integer> payload
    ) {
        User user = authService.getCurrentUser();
        int direction = payload.getOrDefault("direction", 1);
        return ResponseEntity.ok(checklistService.reorderItem(user, id, direction));
    }
}

