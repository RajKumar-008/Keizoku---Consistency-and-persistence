package com.dayforge.service;

import com.dayforge.dto.ReportSummaryDto;
import com.dayforge.entity.ChecklistItem;
import com.dayforge.entity.DailyRecord;
import com.dayforge.entity.Streak;
import com.dayforge.entity.User;
import com.dayforge.repository.ChecklistItemRepository;
import com.dayforge.repository.DailyRecordRepository;
import com.dayforge.repository.StreakRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class ReportService {

    private final DailyRecordRepository dailyRecordRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final StreakRepository streakRepository;

    public ReportService(
        DailyRecordRepository dailyRecordRepository,
        ChecklistItemRepository checklistItemRepository,
        StreakRepository streakRepository
    ) {
        this.dailyRecordRepository = dailyRecordRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.streakRepository = streakRepository;
    }

    private LocalDate getUserToday(User user) {
        try {
            String tz = (user != null && user.getTimezone() != null && !user.getTimezone().isBlank())
                    ? user.getTimezone()
                    : "Asia/Kolkata";
            return LocalDate.now(ZoneId.of(tz));
        } catch (Exception e) {
            return LocalDate.now(ZoneId.of("Asia/Kolkata"));
        }
    }

    public ReportSummaryDto getSummary(User user) {
        ReportSummaryDto dto = new ReportSummaryDto();

        // Streaks
        Streak streak = streakRepository.findByUser(user).orElse(new Streak(user));
        dto.setCurrentStreak(streak.getCurrentStreak());
        dto.setLongestStreak(streak.getLongestStreak());

        // Daily records for last 14 days based on user's timezone
        LocalDate today = getUserToday(user);
        LocalDate startDate = today.minusDays(13);
        List<DailyRecord> recentRecords = dailyRecordRepository.findByUserAndRecordDateBetweenOrderByRecordDateAsc(user, startDate, today);

        Map<LocalDate, DailyRecord> recMap = new HashMap<>();
        for (DailyRecord r : recentRecords) {
            recMap.put(r.getRecordDate(), r);
        }

        Map<String, Double> trend = new LinkedHashMap<>();
        for (int i = 13; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            String label = d.getMonthValue() + "/" + d.getDayOfMonth();
            DailyRecord rec = recMap.get(d);
            trend.put(label, rec != null ? rec.getCompletionPercentage() : 0.0);
        }
        dto.setTrend14Days(trend);

        // Today completion %
        DailyRecord todayRecord = recMap.get(today);
        dto.setDailyCompletionPercent(todayRecord != null ? todayRecord.getCompletionPercentage() : 0.0);

        // Overall task counts scoped to this user
        long completedCount = checklistItemRepository.countByUserAndCompletedTrue(user);
        long totalCount = checklistItemRepository.countByUser(user);
        dto.setTotalCompletedTasks((int) completedCount);
        dto.setTotalMissedTasks(Math.max(0, (int) (totalCount - completedCount)));
        dto.setTotalDaysTracked((int) dailyRecordRepository.countByUserAndIsSuccessfulTrue(user));

        // Category distribution - scoped strictly to this user via repository
        Map<String, Integer> catDist = new LinkedHashMap<>();
        List<ChecklistItem> userItems = checklistItemRepository.findByUser(user);
        for (ChecklistItem item : userItems) {
            String cat = item.getCategory() != null ? item.getCategory().name() : "GENERAL";
            catDist.put(cat, catDist.getOrDefault(cat, 0) + 1);
        }
        dto.setCategoryDistribution(catDist);

        return dto;
    }
}
