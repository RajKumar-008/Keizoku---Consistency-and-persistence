package com.dayforge.entity;

import com.dayforge.entity.enums.GoalStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "goals")
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "target_days", nullable = false)
    private int targetDays;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "current_progress_days", nullable = false)
    private int currentProgressDays = 0;

    @Column(name = "completion_percentage", nullable = false)
    private double completionPercentage = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GoalStatus status = GoalStatus.IN_PROGRESS;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Goal() {}

    public Goal(User user, String title, int targetDays, LocalDate startDate) {
        this.user = user;
        this.title = title;
        this.targetDays = targetDays;
        this.startDate = startDate;
        this.targetDate = startDate.plusDays(targetDays);
        this.currentProgressDays = 0;
        this.completionPercentage = 0.0;
        this.status = GoalStatus.IN_PROGRESS;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProgress(int successfulDays) {
        this.currentProgressDays = Math.min(successfulDays, targetDays);
        if (targetDays > 0) {
            this.completionPercentage = Math.round(((double) currentProgressDays / targetDays) * 1000.0) / 10.0;
        }
        if (this.currentProgressDays >= this.targetDays) {
            this.status = GoalStatus.ACHIEVED;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getTargetDays() { return targetDays; }
    public void setTargetDays(int targetDays) { this.targetDays = targetDays; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public int getCurrentProgressDays() { return currentProgressDays; }
    public void setCurrentProgressDays(int currentProgressDays) { this.currentProgressDays = currentProgressDays; }

    public double getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(double completionPercentage) { this.completionPercentage = completionPercentage; }

    public GoalStatus getStatus() { return status; }
    public void setStatus(GoalStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

