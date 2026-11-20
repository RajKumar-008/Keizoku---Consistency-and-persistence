package com.dayforge.dto;

import com.dayforge.entity.enums.HabitCategory;
import jakarta.validation.constraints.NotBlank;

public class UpdateItemDto {
    @NotBlank(message = "Title cannot be blank")
    private String title;

    private HabitCategory category;

    public UpdateItemDto() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public HabitCategory getCategory() { return category; }
    public void setCategory(HabitCategory category) { this.category = category; }
}

