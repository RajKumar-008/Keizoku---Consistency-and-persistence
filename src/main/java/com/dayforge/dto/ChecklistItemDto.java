package com.dayforge.dto;

import com.dayforge.entity.ChecklistItem;
import com.dayforge.entity.enums.HabitCategory;
import java.time.LocalDateTime;

public class ChecklistItemDto {
    private Long id;
    private String title;
    private HabitCategory category;
    private boolean completed;
    private LocalDateTime completedAt;
    private int correctionAttempts;
    private int remainingCorrections;
    private boolean locked;
    private int orderIndex;
    private Long sourceHabitId;

    public ChecklistItemDto() {}

    public ChecklistItemDto(ChecklistItem item) {
        this.id = item.getId();
        this.title = item.getTitle();
        this.category = item.getCategory();
        this.completed = item.isCompleted();
        this.completedAt = item.getCompletedAt();
        this.correctionAttempts = item.getCorrectionAttempts();
        this.remainingCorrections = item.getRemainingCorrections();
        this.locked = item.isLocked();
        this.orderIndex = item.getOrderIndex();
        this.sourceHabitId = item.getSourceHabitId();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public HabitCategory getCategory() { return category; }
    public void setCategory(HabitCategory category) { this.category = category; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public int getCorrectionAttempts() { return correctionAttempts; }
    public void setCorrectionAttempts(int correctionAttempts) { this.correctionAttempts = correctionAttempts; }

    public int getRemainingCorrections() { return remainingCorrections; }
    public void setRemainingCorrections(int remainingCorrections) { this.remainingCorrections = remainingCorrections; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    public Long getSourceHabitId() { return sourceHabitId; }
    public void setSourceHabitId(Long sourceHabitId) { this.sourceHabitId = sourceHabitId; }
}

