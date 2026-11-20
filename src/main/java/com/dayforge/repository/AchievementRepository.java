package com.dayforge.repository;

import com.dayforge.entity.Achievement;
import com.dayforge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    List<Achievement> findByUserOrderByUnlockedAtDesc(User user);
    Optional<Achievement> findByUserAndBadgeKey(User user, String badgeKey);
    boolean existsByUserAndBadgeKey(User user, String badgeKey);
    void deleteByUser(User user);
}

