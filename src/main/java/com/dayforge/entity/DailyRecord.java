package com.dayforge.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "daily_records",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "record_date"})
)
public class DailyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "total_tasks", nullable = false)
    private int totalTasks = 0;

    @Column(name = "completed_tasks", nullable = false)
    private int completedTasks = 0;

    @Column(name = "completion_percentage", nullable = false)
    private double completionPercentage = 0.0;

    @Column(name = "is_successful", nullable = false)
    private boolean isSuccessful = false;

    @OneToMany(mappedBy = "dailyRecord", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC, id ASC")
    private List<ChecklistItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public DailyRecord() {}

    public DailyRecord(User user, LocalDate recordDate) {
        this.user = user;
        this.recordDate = recordDate;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void recalculateStats() {
        if (items == null || items.isEmpty()) {
            this.totalTasks = 0;
            this.completedTasks = 0;
            this.completionPercentage = 0.0;
            this.isSuccessful = false;
        } else {
            this.totalTasks = items.size();
            long count = items.stream().filter(i -> i != null && i.isCompleted()).count();
            this.completedTasks = (int) count;
            this.completionPercentage = Math.round(((double) completedTasks / totalTasks) * 1000.0) / 10.0;
            this.isSuccessful = (this.completedTasks == this.totalTasks && this.totalTasks > 0);
        }
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }

    public int getTotalTasks() { return totalTasks; }
    public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }

    public int getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(int completedTasks) { this.completedTasks = completedTasks; }

    public double getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(double completionPercentage) { this.completionPercentage = completionPercentage; }

    public boolean isSuccessful() { return isSuccessful; }
    public void setSuccessful(boolean successful) { isSuccessful = successful; }

    public List<ChecklistItem> getItems() { return items; }
    public void setItems(List<ChecklistItem> items) { this.items = items; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

