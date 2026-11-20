package com.dayforge.entity;

import com.dayforge.entity.enums.FrequencyType;
import com.dayforge.entity.enums.HabitCategory;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "habits")
public class Habit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HabitCategory category = HabitCategory.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency_type", nullable = false, length = 30)
    private FrequencyType frequencyType = FrequencyType.DAILY;

    // Comma-separated day numbers (1=Mon, ..., 7=Sun) for CUSTOM_DAYS
    @Column(name = "custom_days", length = 30)
    private String customDays;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "reminder_time", length = 10)
    private String reminderTime;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Habit() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public HabitCategory getCategory() { return category; }
    public void setCategory(HabitCategory category) { this.category = category; }

    public FrequencyType getFrequencyType() { return frequencyType; }
    public void setFrequencyType(FrequencyType frequencyType) { this.frequencyType = frequencyType; }

    public String getCustomDays() { return customDays; }
    public void setCustomDays(String customDays) { this.customDays = customDays; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getReminderTime() { return reminderTime; }
    public void setReminderTime(String reminderTime) { this.reminderTime = reminderTime; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

