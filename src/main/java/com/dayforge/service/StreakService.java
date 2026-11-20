package com.dayforge.service;

import com.dayforge.dto.AchievementDto;
import com.dayforge.dto.StreakDto;
import com.dayforge.entity.*;
import com.dayforge.repository.AchievementRepository;
import com.dayforge.repository.ChecklistItemRepository;
import com.dayforge.repository.DailyRecordRepository;
import com.dayforge.repository.StreakRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class StreakService {

    private final StreakRepository streakRepository;
    private final DailyRecordRepository dailyRecordRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final AchievementRepository achievementRepository;

    public StreakService(
        StreakRepository streakRepository,
        DailyRecordRepository dailyRecordRepository,
        ChecklistItemRepository checklistItemRepository,
        AchievementRepository achievementRepository
    ) {
        this.streakRepository = streakRepository;
        this.dailyRecordRepository = dailyRecordRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.achievementRepository = achievementRepository;
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

    public Streak getOrCreateStreak(User user) {
        return streakRepository.findByUser(user)
                .orElseGet(() -> streakRepository.save(new Streak(user)));
    }

    public StreakDto getStreakDto(User user) {
        Streak streak = getOrCreateStreak(user);
        return new StreakDto(streak);
    }

    @Transactional
    public StreakDto recalculateStreak(User user) {
        Streak streak = getOrCreateStreak(user);

        // Fetch all historical daily records ordered descending
        List<DailyRecord> records = dailyRecordRepository.findByUserOrderByRecordDateDesc(user);
        if (records.isEmpty()) {
            streak.setCurrentStreak(0);
            streak.setTotalSuccessfulDays(0);
            streak.setTotalMissedDays(0);
            streak.setUpdatedAt(LocalDateTime.now());
            return new StreakDto(streakRepository.save(streak));
        }

        // Map by date
        Map<LocalDate, DailyRecord> recordMap = new HashMap<>();
        int successfulCount = 0;
        int missedCount = 0;

        for (DailyRecord r : records) {
            recordMap.put(r.getRecordDate(), r);
            if (r.isSuccessful()) {
                successfulCount++;
            } else if (r.getTotalTasks() > 0) {
                missedCount++;
            }
        }

        streak.setTotalSuccessfulDays(successfulCount);
        streak.setTotalMissedDays(missedCount);

        // Calculate current consecutive streak backwards from user's today
        LocalDate today = getUserToday(user);
        DailyRecord todayRecord = recordMap.get(today);

        int currentConsecutive = 0;
        LocalDate cursor;

        // If today is successful, streak includes today
        if (todayRecord != null && todayRecord.isSuccessful()) {
            currentConsecutive = 1;
            cursor = today.minusDays(1);
        } else {
            // Check if yesterday was successful (streak can still be active today if today is not ended)
            cursor = today.minusDays(1);
        }

        while (true) {
            DailyRecord r = recordMap.get(cursor);
            if (r != null && r.isSuccessful()) {
                currentConsecutive++;
                cursor = cursor.minusDays(1);
            } else {
                break;
            }
        }

        streak.setCurrentStreak(currentConsecutive);

        // Calculate all-time longest streak
        List<LocalDate> sortedDates = new ArrayList<>(recordMap.keySet());
        Collections.sort(sortedDates);

        int maxStreak = 0;
        int runningStreak = 0;
        LocalDate prevDate = null;

        for (LocalDate d : sortedDates) {
            DailyRecord rec = recordMap.get(d);
            if (rec != null && rec.isSuccessful()) {
                if (prevDate != null && ChronoUnit.DAYS.between(prevDate, d) == 1) {
                    runningStreak++;
                } else {
                    runningStreak = 1;
                }
                maxStreak = Math.max(maxStreak, runningStreak);
                prevDate = d;
            } else {
                runningStreak = 0;
                prevDate = null;
            }
        }

        int allTimeBest = Math.max(streak.getLongestStreak(), Math.max(maxStreak, currentConsecutive));
        streak.setLongestStreak(allTimeBest);

        streak.setLastCalculatedDate(today);
        streak.setUpdatedAt(LocalDateTime.now());
        streak = streakRepository.save(streak);

        // Check milestones
        checkAndAwardAchievements(user, streak);

        return new StreakDto(streak);
    }

    @Transactional
    public void checkAndAwardAchievements(User user, Streak streak) {
        long completedTasks = checklistItemRepository.countByUserAndCompletedTrue(user);
        if (completedTasks > 0) {
            unlockBadge(user, "FIRST_STEP", "First Step Taken", "Completed your very first daily habit activity.", "🌱");
        }

        int curr = streak.getCurrentStreak();
        int longest = streak.getLongestStreak();
        int best = Math.max(curr, longest);

        if (best >= 7) {
            unlockBadge(user, "STREAK_7", "Consistency Starter", "Forged a solid 7-day unbroken habit streak.", "🔥");
        }
        if (best >= 14) {
            unlockBadge(user, "STREAK_14", "Discipline Builder", "Maintained 14 consecutive days of 100% completion.", "⚡");
        }
        if (best >= 28) {
            unlockBadge(user, "STREAK_28", "Habit Master", "Reached 28 consecutive days – habits are becoming automatic.", "🏆");
        }
        if (best >= 60) {
            unlockBadge(user, "STREAK_60", "Unstoppable", "Completed 60 days unbroken streak. True consistency.", "💎");
        }
        if (best >= 100) {
            unlockBadge(user, "STREAK_100", "Legendary", "100 consecutive days of mastery. A transformed life.", "👑");
        }

        if (streak.getTotalSuccessfulDays() >= 30) {
            unlockBadge(user, "PERFECT_MONTH", "Canopy of Excellence", "Logged 30 total successful 100% days.", "🌳");
        }
    }

    private void unlockBadge(User user, String key, String title, String desc, String icon) {
        if (!achievementRepository.existsByUserAndBadgeKey(user, key)) {
            Achievement badge = new Achievement(user, key, title, desc, icon);
            achievementRepository.save(badge);
        }
    }

    public List<AchievementDto> getAllAchievements(User user) {
        // Standard catalogue of badges
        List<AchievementDto> all = new ArrayList<>();
        Map<String, Achievement> unlockedMap = new HashMap<>();

        for (Achievement a : achievementRepository.findByUserOrderByUnlockedAtDesc(user)) {
            unlockedMap.put(a.getBadgeKey(), a);
        }

        String[][] catalog = {
            {"FIRST_STEP", "First Step Taken", "Completed your very first daily habit activity.", "🌱"},
            {"STREAK_7", "Consistency Starter", "Forged a solid 7-day unbroken habit streak.", "🔥"},
            {"STREAK_14", "Discipline Builder", "Maintained 14 consecutive days of 100% completion.", "⚡"},
            {"STREAK_28", "Habit Master", "Reached 28 consecutive days – habits are automatic.", "🏆"},
            {"STREAK_60", "Unstoppable", "Completed 60 days unbroken streak. True consistency.", "💎"},
            {"STREAK_100", "Legendary", "100 consecutive days of mastery.", "👑"},
            {"PERFECT_MONTH", "Canopy of Excellence", "Logged 30 total successful 100% days.", "🌳"}
        };

        for (String[] item : catalog) {
            String key = item[0];
            if (unlockedMap.containsKey(key)) {
                all.add(new AchievementDto(unlockedMap.get(key)));
            } else {
                all.add(new AchievementDto(key, item[1], item[2], item[3], null, false));
            }
        }

        return all;
    }
}
