package com.dayforge.controller;

import com.dayforge.dto.ReportSummaryDto;
import com.dayforge.entity.DailyRecord;
import com.dayforge.entity.User;
import com.dayforge.repository.DailyRecordRepository;
import com.dayforge.service.AuthService;
import com.dayforge.service.ChecklistService;
import com.dayforge.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final AuthService authService;
    private final DailyRecordRepository dailyRecordRepository;
    private final ChecklistService checklistService;

    public ReportController(
        ReportService reportService,
        AuthService authService,
        DailyRecordRepository dailyRecordRepository,
        ChecklistService checklistService
    ) {
        this.reportService = reportService;
        this.authService = authService;
        this.dailyRecordRepository = dailyRecordRepository;
        this.checklistService = checklistService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ReportSummaryDto> getSummary() {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(reportService.getSummary(user));
    }

    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory() {
        User user = authService.getCurrentUser();
        LocalDate today = checklistService.getUserToday(user);

        List<DailyRecord> records = dailyRecordRepository.findByUserOrderByRecordDateDesc(user);
        List<Map<String, Object>> result = new ArrayList<>();

        for (DailyRecord r : records) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("date", r.getRecordDate());
            map.put("totalTasks", r.getTotalTasks());
            map.put("completedTasks", r.getCompletedTasks());
            map.put("completionPercentage", r.getCompletionPercentage());
            map.put("isSuccessful", r.isSuccessful());
            map.put("isToday", r.getRecordDate().isEqual(today));
            map.put("isPast", r.getRecordDate().isBefore(today));
            map.put("isFuture", r.getRecordDate().isAfter(today));
            result.add(map);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/month")
    public ResponseEntity<List<Map<String, Object>>> getMonthData(
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) Integer month
    ) {
        User user = authService.getCurrentUser();
        LocalDate today = checklistService.getUserToday(user);
        int resolvedYear = (year != null) ? year : today.getYear();
        int resolvedMonth = (month != null) ? month : today.getMonthValue();
        LocalDate start = LocalDate.of(resolvedYear, resolvedMonth, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        List<DailyRecord> monthRecords = dailyRecordRepository.findByUserAndRecordDateBetweenOrderByRecordDateAsc(user, start, end);
        List<Map<String, Object>> monthResult = new ArrayList<>();

        for (DailyRecord r : monthRecords) {
            Map<String, Object> map = new HashMap<>();
            map.put("date", r.getRecordDate());
            map.put("total", r.getTotalTasks());
            map.put("completed", r.getCompletedTasks());
            map.put("percentage", r.getCompletionPercentage());
            map.put("successful", r.isSuccessful());
            monthResult.add(map);
        }

        return ResponseEntity.ok(monthResult);
    }
}
