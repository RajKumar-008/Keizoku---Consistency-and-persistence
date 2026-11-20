package com.dayforge.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DailyChecklistResponse {
    private LocalDate date;
    private int totalTasks;
    private int completedTasks;
    private double completionPercentage;
    private boolean isSuccessful;
    private boolean isPast;
    private boolean isToday;
    private boolean isFuture;
    private List<ChecklistItemDto> items = new ArrayList<>();

    public DailyChecklistResponse() {}

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public int getTotalTasks() { return totalTasks; }
    public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }

    public int getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(int completedTasks) { this.completedTasks = completedTasks; }

    public double getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(double completionPercentage) { this.completionPercentage = completionPercentage; }

    public boolean isSuccessful() { return isSuccessful; }
    public void setSuccessful(boolean successful) { isSuccessful = successful; }

    public boolean isPast() { return isPast; }
    public void setPast(boolean past) { isPast = past; }

    public boolean isToday() { return isToday; }
    public void setToday(boolean today) { isToday = today; }

    public boolean isFuture() { return isFuture; }
    public void setFuture(boolean future) { isFuture = future; }

    public List<ChecklistItemDto> getItems() { return items; }
    public void setItems(List<ChecklistItemDto> items) { this.items = items; }
}

