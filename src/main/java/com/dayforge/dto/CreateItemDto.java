package com.dayforge.dto;

import com.dayforge.entity.enums.HabitCategory;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public class CreateItemDto {
    @NotBlank(message = "Title is required")
    private String title;

    private HabitCategory category = HabitCategory.GENERAL;

    private LocalDate date;

    public CreateItemDto() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public HabitCategory getCategory() { return category; }
    public void setCategory(HabitCategory category) { this.category = category; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}

