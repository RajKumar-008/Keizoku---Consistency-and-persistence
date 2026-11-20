package com.dayforge.dto;

import com.dayforge.entity.Habit;
import com.dayforge.entity.enums.FrequencyType;
import com.dayforge.entity.enums.HabitCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class HabitDto {
    private Long id;

    @NotBlank(message = "Habit title is required")
    private String title;

    private HabitCategory category = HabitCategory.GENERAL;

    @NotNull(message = "Frequency type is required")
    private FrequencyType frequencyType = FrequencyType.DAILY;

    private String customDays;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    private boolean active = true;

    private String reminderTime;

    public HabitDto() {}

    public HabitDto(Habit habit) {
        this.id = habit.getId();
        this.title = habit.getTitle();
        this.category = habit.getCategory();
        this.frequencyType = habit.getFrequencyType();
        this.customDays = habit.getCustomDays();
        this.startDate = habit.getStartDate();
        this.endDate = habit.getEndDate();
        this.active = habit.isActive();
        this.reminderTime = habit.getReminderTime();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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
}

