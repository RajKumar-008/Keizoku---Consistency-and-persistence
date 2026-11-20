package com.dayforge.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "achievements",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "badge_key"})
)
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "badge_key", nullable = false, length = 50)
    private String badgeKey;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 250)
    private String description;

    @Column(nullable = false, length = 20)
    private String icon;

    @Column(name = "unlocked_at", nullable = false)
    private LocalDateTime unlockedAt = LocalDateTime.now();

    public Achievement() {}

    public Achievement(User user, String badgeKey, String title, String description, String icon) {
        this.user = user;
        this.badgeKey = badgeKey;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.unlockedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

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
}

