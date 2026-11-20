package com.dayforge.dto;

import com.dayforge.entity.NotificationPreference;

public class NotificationPrefDto {
    private boolean dailyReminderEnabled;
    private String dailyReminderTime;
    private boolean pendingTaskReminder;
    private boolean streakWarningEnabled;
    private boolean quietHoursEnabled;
    private String quietHoursStart;
    private String quietHoursEnd;
    private boolean browserNotificationsEnabled;

    public NotificationPrefDto() {}

    public NotificationPrefDto(NotificationPreference pref) {
        if (pref != null) {
            this.dailyReminderEnabled = pref.isDailyReminderEnabled();
            this.dailyReminderTime = pref.getDailyReminderTime();
            this.pendingTaskReminder = pref.isPendingTaskReminder();
            this.streakWarningEnabled = pref.isStreakWarningEnabled();
            this.quietHoursEnabled = pref.isQuietHoursEnabled();
            this.quietHoursStart = pref.getQuietHoursStart();
            this.quietHoursEnd = pref.getQuietHoursEnd();
            this.browserNotificationsEnabled = pref.isBrowserNotificationsEnabled();
        }
    }

    public boolean isDailyReminderEnabled() { return dailyReminderEnabled; }
    public void setDailyReminderEnabled(boolean dailyReminderEnabled) { this.dailyReminderEnabled = dailyReminderEnabled; }

    public String getDailyReminderTime() { return dailyReminderTime; }
    public void setDailyReminderTime(String dailyReminderTime) { this.dailyReminderTime = dailyReminderTime; }

    public boolean isPendingTaskReminder() { return pendingTaskReminder; }
    public void setPendingTaskReminder(boolean pendingTaskReminder) { this.pendingTaskReminder = pendingTaskReminder; }

    public boolean isStreakWarningEnabled() { return streakWarningEnabled; }
    public void setStreakWarningEnabled(boolean streakWarningEnabled) { this.streakWarningEnabled = streakWarningEnabled; }

    public boolean isQuietHoursEnabled() { return quietHoursEnabled; }
    public void setQuietHoursEnabled(boolean quietHoursEnabled) { this.quietHoursEnabled = quietHoursEnabled; }

    public String getQuietHoursStart() { return quietHoursStart; }
    public void setQuietHoursStart(String quietHoursStart) { this.quietHoursStart = quietHoursStart; }

    public String getQuietHoursEnd() { return quietHoursEnd; }
    public void setQuietHoursEnd(String quietHoursEnd) { this.quietHoursEnd = quietHoursEnd; }

    public boolean isBrowserNotificationsEnabled() { return browserNotificationsEnabled; }
    public void setBrowserNotificationsEnabled(boolean browserNotificationsEnabled) { this.browserNotificationsEnabled = browserNotificationsEnabled; }
}

