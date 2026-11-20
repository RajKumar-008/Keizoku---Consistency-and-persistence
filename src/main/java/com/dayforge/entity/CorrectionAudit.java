package com.dayforge.entity;

import com.dayforge.entity.enums.AuditAction;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "correction_audits")
public class CorrectionAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checklist_item_id", nullable = false)
    private ChecklistItem checklistItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditAction action;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "logged_at", nullable = false)
    private LocalDateTime loggedAt = LocalDateTime.now();

    public CorrectionAudit() {}

    public CorrectionAudit(ChecklistItem checklistItem, User user, LocalDate recordDate, AuditAction action, int attemptNumber) {
        this.checklistItem = checklistItem;
        this.user = user;
        this.recordDate = recordDate;
        this.action = action;
        this.attemptNumber = attemptNumber;
        this.loggedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ChecklistItem getChecklistItem() { return checklistItem; }
    public void setChecklistItem(ChecklistItem checklistItem) { this.checklistItem = checklistItem; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }

    public AuditAction getAction() { return action; }
    public void setAction(AuditAction action) { this.action = action; }

    public int getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }

    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
}

