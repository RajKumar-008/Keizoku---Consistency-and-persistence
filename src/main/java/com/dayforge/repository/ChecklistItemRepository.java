package com.dayforge.repository;

import com.dayforge.entity.ChecklistItem;
import com.dayforge.entity.DailyRecord;
import com.dayforge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {
    List<ChecklistItem> findByDailyRecordOrderByOrderIndexAscIdAsc(DailyRecord dailyRecord);

    Optional<ChecklistItem> findByIdAndUser(Long id, User user);

    List<ChecklistItem> findByUser(User user);

    @Query("SELECT MAX(c.orderIndex) FROM ChecklistItem c WHERE c.dailyRecord = :dailyRecord")
    Integer findMaxOrderIndex(@Param("dailyRecord") DailyRecord dailyRecord);

    long countByUserAndCompletedTrue(User user);
    long countByUser(User user);
    void deleteByUser(User user);
}
