package com.dayforge.service;

import com.dayforge.dto.NotificationPrefDto;
import com.dayforge.entity.DailyRecord;
import com.dayforge.entity.NotificationPreference;
import com.dayforge.entity.Streak;
import com.dayforge.entity.User;
import com.dayforge.repository.DailyRecordRepository;
import com.dayforge.repository.NotificationPreferenceRepository;
import com.dayforge.repository.StreakRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class NotificationService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final DailyRecordRepository dailyRecordRepository;
    private final StreakRepository streakRepository;

    public NotificationService(
        NotificationPreferenceRepository preferenceRepository,
        DailyRecordRepository dailyRecordRepository,
        StreakRepository streakRepository
    ) {
        this.preferenceRepository = preferenceRepository;
        this.dailyRecordRepository = dailyRecordRepository;
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

    public NotificationPrefDto getPreferences(User user) {
        NotificationPreference pref = preferenceRepository.findByUser(user)
                .orElseGet(() -> preferenceRepository.save(new NotificationPreference(user)));
        return new NotificationPrefDto(pref);
    }

    @Transactional
    public NotificationPrefDto updatePreferences(User user, NotificationPrefDto dto) {
        NotificationPreference pref = preferenceRepository.findByUser(user)
                .orElseGet(() -> new NotificationPreference(user));

        pref.setDailyReminderEnabled(dto.isDailyReminderEnabled());
        pref.setDailyReminderTime(dto.getDailyReminderTime());
        pref.setPendingTaskReminder(dto.isPendingTaskReminder());
        pref.setStreakWarningEnabled(dto.isStreakWarningEnabled());
        pref.setQuietHoursEnabled(dto.isQuietHoursEnabled());
        pref.setQuietHoursStart(dto.getQuietHoursStart());
        pref.setQuietHoursEnd(dto.getQuietHoursEnd());
        pref.setBrowserNotificationsEnabled(dto.isBrowserNotificationsEnabled());
        pref.setUpdatedAt(LocalDateTime.now());

        pref = preferenceRepository.save(pref);
        return new NotificationPrefDto(pref);
    }

    public List<Map<String, Object>> getLiveNotifications(User user) {
        List<Map<String, Object>> notices = new ArrayList<>();
        LocalDate today = getUserToday(user);

        // 1. Daily Checklist Progress Notice
        Optional<DailyRecord> todayRecOpt = dailyRecordRepository.findByUserAndRecordDate(user, today);
        if (todayRecOpt.isPresent()) {
            DailyRecord rec = todayRecOpt.get();
            int remaining = rec.getTotalTasks() - rec.getCompletedTasks();
            if (rec.getTotalTasks() > 0 && remaining > 0) {
                Map<String, Object> notice = new HashMap<>();
                notice.put("id", 1);
                notice.put("type", "PENDING_TASKS");
                notice.put("title", "Pending Daily Tasks");
                notice.put("message", "You have " + remaining + " remaining activities on your checklist today.");
                notice.put("time", "Today");
                notice.put("level", "info");
                notices.add(notice);
            } else if (rec.getTotalTasks() > 0 && remaining == 0) {
                Map<String, Object> notice = new HashMap<>();
                notice.put("id", 2);
                notice.put("type", "COMPLETED_DAY");
                notice.put("title", "100% Checklist Complete!");
                notice.put("message", "Congratulations! All planned activities for today have been conquered.");
                notice.put("time", "Today");
                notice.put("level", "success");
                notices.add(notice);
            }
        }

        // 2. Streak Alert Notice
        Optional<Streak> streakOpt = streakRepository.findByUser(user);
        if (streakOpt.isPresent() && streakOpt.get().getCurrentStreak() > 0) {
            Map<String, Object> notice = new HashMap<>();
            notice.put("id", 3);
            notice.put("type", "STREAK");
            notice.put("title", "Active Streak: " + streakOpt.get().getCurrentStreak() + " Days");
            notice.put("message", "Keep the momentum going! Don't let your daily consistency lapse.");
            notice.put("time", "Ongoing");
            notice.put("level", "warning");
            notices.add(notice);
        }

        return notices;
    }
}
