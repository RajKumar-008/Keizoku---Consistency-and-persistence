package com.dayforge.repository;

import com.dayforge.entity.Goal;
import com.dayforge.entity.User;
import com.dayforge.entity.enums.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUserOrderByCreatedAtDesc(User user);
    List<Goal> findByUserAndStatusOrderByCreatedAtDesc(User user, GoalStatus status);
    void deleteByUser(User user);
}

