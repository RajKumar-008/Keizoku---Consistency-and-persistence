package com.dayforge.dto;

import com.dayforge.entity.Achievement;
import java.time.LocalDateTime;

public class AchievementDto {
    private String badgeKey;
    private String title;
    private String description;
    private String icon;
    private LocalDateTime unlockedAt;
    private boolean unlocked;

    public AchievementDto() {}

    public AchievementDto(String badgeKey, String title, String description, String icon, LocalDateTime unlockedAt, boolean unlocked) {
        this.badgeKey = badgeKey;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.unlockedAt = unlockedAt;
        this.unlocked = unlocked;
    }

    public AchievementDto(Achievement achievement) {
        this.badgeKey = achievement.getBadgeKey();
        this.title = achievement.getTitle();
        this.description = achievement.getDescription();
        this.icon = achievement.getIcon();
        this.unlockedAt = achievement.getUnlockedAt();
        this.unlocked = true;
    }

    public String getBadgeKey() { return badgeKey; }
    public void setBadgeKey(String badgeKey) { this.badgeKey = badgeKey; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public LocalDateTime getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(LocalDateTime unlockedAt) { this.unlockedAt = unlockedAt; }

    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
}

