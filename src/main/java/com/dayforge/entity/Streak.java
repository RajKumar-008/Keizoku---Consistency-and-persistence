package com.dayforge.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "streaks")
public class Streak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak = 0;

    @Column(name = "longest_streak", nullable = false)
    private int longestStreak = 0;

    @Column(name = "total_successful_days", nullable = false)
    private int totalSuccessfulDays = 0;

    @Column(name = "total_missed_days", nullable = false)
    private int totalMissedDays = 0;

    @Column(name = "last_calculated_date")
    private LocalDate lastCalculatedDate;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Streak() {}

    public Streak(User user) {
        this.user = user;
        this.currentStreak = 0;
        this.longestStreak = 0;
        this.totalSuccessfulDays = 0;
        this.totalMissedDays = 0;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public int getLongestStreak() { return longestStreak; }
    public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }

    public int getTotalSuccessfulDays() { return totalSuccessfulDays; }
    public void setTotalSuccessfulDays(int totalSuccessfulDays) { this.totalSuccessfulDays = totalSuccessfulDays; }

    public int getTotalMissedDays() { return totalMissedDays; }
    public void setTotalMissedDays(int totalMissedDays) { this.totalMissedDays = totalMissedDays; }

    public LocalDate getLastCalculatedDate() { return lastCalculatedDate; }
    public void setLastCalculatedDate(LocalDate lastCalculatedDate) { this.lastCalculatedDate = lastCalculatedDate; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

