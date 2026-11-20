package com.dayforge.repository;

import com.dayforge.entity.ChecklistItem;
import com.dayforge.entity.CorrectionAudit;
import com.dayforge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CorrectionAuditRepository extends JpaRepository<CorrectionAudit, Long> {
    List<CorrectionAudit> findByChecklistItemOrderByLoggedAtDesc(ChecklistItem checklistItem);
    List<CorrectionAudit> findByUserAndRecordDateOrderByLoggedAtDesc(User user, LocalDate recordDate);
    void deleteByUser(User user);
}

