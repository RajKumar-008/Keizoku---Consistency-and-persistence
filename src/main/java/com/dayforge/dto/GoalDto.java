package com.dayforge.dto;

import com.dayforge.entity.Goal;
import com.dayforge.entity.enums.GoalStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class GoalDto {
    private Long id;

    @NotBlank(message = "Goal title is required")
    private String title;

    @Min(value = 1, message = "Target days must be at least 1")
    private int targetDays;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate targetDate;
    private int currentProgressDays;
    private long remainingDays;
    private double completionPercentage;
    private GoalStatus status;

    public GoalDto() {}

    public GoalDto(Goal goal, LocalDate currentDate) {
        this.id = goal.getId();
        this.title = goal.getTitle();
        this.targetDays = goal.getTargetDays();
        this.startDate = goal.getStartDate();
        this.targetDate = goal.getTargetDate();
        this.currentProgressDays = goal.getCurrentProgressDays();
        this.completionPercentage = goal.getCompletionPercentage();
        this.status = goal.getStatus();

        if (goal.getTargetDate() != null && currentDate != null) {
            long diff = ChronoUnit.DAYS.between(currentDate, goal.getTargetDate());
            this.remainingDays = Math.max(0, diff);
        } else {
            this.remainingDays = 0;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public long getRemainingDays() { return remainingDays; }
    public void setRemainingDays(long remainingDays) { this.remainingDays = remainingDays; }

    public double getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(double completionPercentage) { this.completionPercentage = completionPercentage; }

    public GoalStatus getStatus() { return status; }
    public void setStatus(GoalStatus status) { this.status = status; }
}

