package com.dayforge.entity;

import com.dayforge.entity.enums.HabitCategory;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "checklist_items")
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_record_id", nullable = false)
    private DailyRecord dailyRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HabitCategory category = HabitCategory.GENERAL;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Strict 3-correction protection mechanism
    @Column(name = "correction_attempts", nullable = false)
    private int correctionAttempts = 0;

    @Column(name = "is_locked", nullable = false)
    private boolean locked = false;

    @Column(name = "order_index", nullable = false)
    private int orderIndex = 0;

    @Column(name = "source_habit_id")
    private Long sourceHabitId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public ChecklistItem() {}

    public ChecklistItem(DailyRecord dailyRecord, User user, String title, HabitCategory category, int orderIndex, Long sourceHabitId) {
        this.dailyRecord = dailyRecord;
        this.user = user;
        this.title = title;
        this.category = (category != null) ? category : HabitCategory.GENERAL;
        this.orderIndex = orderIndex;
        this.sourceHabitId = sourceHabitId;
        this.completed = false;
        this.correctionAttempts = 0;
        this.locked = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public int getRemainingCorrections() {
        return Math.max(0, 3 - correctionAttempts);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DailyRecord getDailyRecord() { return dailyRecord; }
    public void setDailyRecord(DailyRecord dailyRecord) { this.dailyRecord = dailyRecord; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

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

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    public Long getSourceHabitId() { return sourceHabitId; }
    public void setSourceHabitId(Long sourceHabitId) { this.sourceHabitId = sourceHabitId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

