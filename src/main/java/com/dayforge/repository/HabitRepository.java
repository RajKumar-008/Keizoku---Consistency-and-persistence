package com.dayforge.repository;

import com.dayforge.entity.Habit;
import com.dayforge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HabitRepository extends JpaRepository<Habit, Long> {
    List<Habit> findByUserOrderByIdDesc(User user);
    List<Habit> findByUserAndActiveTrue(User user);

    @Query("SELECT h FROM Habit h WHERE h.user = :user AND h.active = true AND h.startDate <= :date AND (h.endDate IS NULL OR h.endDate >= :date)")
    List<Habit> findActiveHabitsForDate(@Param("user") User user, @Param("date") LocalDate date);

    void deleteByUser(User user);
}

