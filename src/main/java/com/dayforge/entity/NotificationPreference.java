package com.dayforge.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "daily_reminder_enabled", nullable = false)
    private boolean dailyReminderEnabled = true;

    @Column(name = "daily_reminder_time", nullable = false, length = 10)
    private String dailyReminderTime = "08:00";

    @Column(name = "pending_task_reminder", nullable = false)
    private boolean pendingTaskReminder = true;

    @Column(name = "streak_warning_enabled", nullable = false)
    private boolean streakWarningEnabled = true;

    @Column(name = "quiet_hours_enabled", nullable = false)
    private boolean quietHoursEnabled = true;

    @Column(name = "quiet_hours_start", nullable = false, length = 10)
    private String quietHoursStart = "22:00";

    @Column(name = "quiet_hours_end", nullable = false, length = 10)
    private String quietHoursEnd = "07:00";

    @Column(name = "browser_notifications_enabled", nullable = false)
    private boolean browserNotificationsEnabled = false;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public NotificationPreference() {}

    public NotificationPreference(User user) {
        this.user = user;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

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

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

