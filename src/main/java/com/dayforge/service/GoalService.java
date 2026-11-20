package com.dayforge.service;

import com.dayforge.dto.GoalDto;
import com.dayforge.entity.DailyRecord;
import com.dayforge.entity.Goal;
import com.dayforge.entity.User;
import com.dayforge.entity.enums.GoalStatus;
import com.dayforge.exception.ResourceNotFoundException;
import com.dayforge.repository.DailyRecordRepository;
import com.dayforge.repository.GoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final DailyRecordRepository dailyRecordRepository;

    public GoalService(GoalRepository goalRepository, DailyRecordRepository dailyRecordRepository) {
        this.goalRepository = goalRepository;
        this.dailyRecordRepository = dailyRecordRepository;
    }

    private LocalDate getUserToday(User user) {
        try {
            String tz = (user != null && user.getTimezone() != null && !user.getTimezone().isBlank())
                    ? user.getTimezone()
                    : "Asia/Kolkata";
            return LocalDate.now(ZoneId.of(tz));
        } catch (Exception e) {
            return LocalDate.now(ZoneId.of("Asia/Kolkata"));
        }
    }

    public List<GoalDto> getUserGoals(User user) {
        LocalDate today = getUserToday(user);
        return goalRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(g -> new GoalDto(g, today))
                .collect(Collectors.toList());
    }

    @Transactional
    public GoalDto createGoal(User user, GoalDto dto) {
        LocalDate today = getUserToday(user);
        LocalDate start = (dto.getStartDate() != null) ? dto.getStartDate() : today;
        Goal goal = new Goal(user, dto.getTitle().trim(), dto.getTargetDays(), start);
        goal = goalRepository.save(goal);
        syncSingleGoalProgress(user, goal, today);
        return new GoalDto(goal, today);
    }

    @Transactional
    public void deleteGoal(User user, Long goalId) {
        Goal goal = goalRepository.findById(Objects.requireNonNull(goalId))
                .filter(g -> g.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found: " + goalId));
        goalRepository.delete(Objects.requireNonNull(goal));
    }

    @Transactional
    public void syncGoalProgress(User user) {
        LocalDate today = getUserToday(user);
        List<Goal> inProgressGoals = goalRepository.findByUserAndStatusOrderByCreatedAtDesc(user, GoalStatus.IN_PROGRESS);
        for (Goal goal : inProgressGoals) {
            syncSingleGoalProgress(user, goal, today);
        }
    }

    private void syncSingleGoalProgress(User user, Goal goal, LocalDate today) {
        LocalDate start = goal.getStartDate();
        LocalDate end = goal.getTargetDate();
        LocalDate evalEnd = today.isBefore(end) ? today : end;

        List<DailyRecord> records = dailyRecordRepository.findByUserAndRecordDateBetweenOrderByRecordDateAsc(user, start, evalEnd);
        int successfulDays = (int) records.stream().filter(r -> r != null && r.isSuccessful()).count();

        goal.updateProgress(successfulDays);
        goalRepository.save(goal);
    }
}
