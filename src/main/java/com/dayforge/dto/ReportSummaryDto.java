package com.dayforge.dto;

import java.util.HashMap;
import java.util.Map;

public class ReportSummaryDto {
    private double dailyCompletionPercent;
    private int currentStreak;
    private int longestStreak;
    private int totalCompletedTasks;
    private int totalMissedTasks;
    private int totalDaysTracked;
    private Map<String, Double> trend14Days = new HashMap<>();
    private Map<String, Integer> categoryDistribution = new HashMap<>();

    public ReportSummaryDto() {}

    public double getDailyCompletionPercent() { return dailyCompletionPercent; }
    public void setDailyCompletionPercent(double dailyCompletionPercent) { this.dailyCompletionPercent = dailyCompletionPercent; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public int getLongestStreak() { return longestStreak; }
    public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }

    public int getTotalCompletedTasks() { return totalCompletedTasks; }
    public void setTotalCompletedTasks(int totalCompletedTasks) { this.totalCompletedTasks = totalCompletedTasks; }

    public int getTotalMissedTasks() { return totalMissedTasks; }
    public void setTotalMissedTasks(int totalMissedTasks) { this.totalMissedTasks = totalMissedTasks; }

    public int getTotalDaysTracked() { return totalDaysTracked; }
    public void setTotalDaysTracked(int totalDaysTracked) { this.totalDaysTracked = totalDaysTracked; }

    public Map<String, Double> getTrend14Days() { return trend14Days; }
    public void setTrend14Days(Map<String, Double> trend14Days) { this.trend14Days = trend14Days; }

    public Map<String, Integer> getCategoryDistribution() { return categoryDistribution; }
    public void setCategoryDistribution(Map<String, Integer> categoryDistribution) { this.categoryDistribution = categoryDistribution; }
}

