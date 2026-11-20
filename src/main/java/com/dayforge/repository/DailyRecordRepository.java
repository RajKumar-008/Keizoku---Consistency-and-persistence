package com.dayforge.repository;

import com.dayforge.entity.DailyRecord;
import com.dayforge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyRecordRepository extends JpaRepository<DailyRecord, Long> {
    Optional<DailyRecord> findByUserAndRecordDate(User user, LocalDate recordDate);

    List<DailyRecord> findByUserAndRecordDateBetweenOrderByRecordDateAsc(User user, LocalDate startDate, LocalDate endDate);

    List<DailyRecord> findByUserOrderByRecordDateDesc(User user);

    @Query("SELECT d FROM DailyRecord d WHERE d.user = :user AND d.isSuccessful = true ORDER BY d.recordDate DESC")
    List<DailyRecord> findSuccessfulDays(@Param("user") User user);

    long countByUserAndIsSuccessfulTrue(User user);
    void deleteByUser(User user);
}

