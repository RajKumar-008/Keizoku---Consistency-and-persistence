package com.dayforge.dto;

import com.dayforge.entity.Streak;
import java.time.LocalDate;

public class StreakDto {
    private int currentStreak;
    private int longestStreak;
    private int totalSuccessfulDays;
    private int totalMissedDays;
    private LocalDate lastCalculatedDate;

    public StreakDto() {}

    public StreakDto(Streak streak) {
        if (streak != null) {
            this.currentStreak = streak.getCurrentStreak();
            this.longestStreak = streak.getLongestStreak();
            this.totalSuccessfulDays = streak.getTotalSuccessfulDays();
            this.totalMissedDays = streak.getTotalMissedDays();
            this.lastCalculatedDate = streak.getLastCalculatedDate();
        }
    }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public int getLongestStreak() { return longestStreak; }
    public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }

    public int getTotalSuccessfulDays() { return totalSuccessfulDays; }
    public void setTotalSuccessfulDays(int totalSuccessfulDays) { this.totalSuccessfulDays = totalSuccessfulDays; }

    public int getTotalMissedDays() { return totalMissedDays; }
    public void setTotalMissedDays(int totalMissedDays) { this.totalMissedDays = totalMissedDays; }

    public LocalDate getLastCalculatedDate() { return lastCalculatedDate; }
    public void setLastCalculatedDate(LocalDate lastCalculatedDate) { this.lastCalculatedDate = lastCalculatedDate; }
}

