package com.dayforge.dto;

import com.dayforge.entity.enums.HabitCategory;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public class BatchCreateItemsDto {
    @NotBlank(message = "Activities text is required")
    private String activities;

    private HabitCategory defaultCategory = HabitCategory.GENERAL;

    private LocalDate date;

    public BatchCreateItemsDto() {}

    public String getActivities() { return activities; }
    public void setActivities(String activities) { this.activities = activities; }

    public HabitCategory getDefaultCategory() { return defaultCategory; }
    public void setDefaultCategory(HabitCategory defaultCategory) { this.defaultCategory = defaultCategory; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}

