package com.dayforge.service;

import com.dayforge.dto.HabitDto;
import com.dayforge.entity.Habit;
import com.dayforge.entity.User;
import com.dayforge.exception.ResourceNotFoundException;
import com.dayforge.repository.HabitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class HabitService {

    private final HabitRepository habitRepository;
    private final ChecklistService checklistService;

    public HabitService(HabitRepository habitRepository, ChecklistService checklistService) {
        this.habitRepository = habitRepository;
        this.checklistService = checklistService;
    }

    public List<HabitDto> getUserHabits(User user) {
        return habitRepository.findByUserOrderByIdDesc(user)
                .stream()
                .map(HabitDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public HabitDto createHabit(User user, HabitDto dto) {
        Habit habit = new Habit();
        habit.setUser(user);
        habit.setTitle(dto.getTitle().trim());
        habit.setCategory(dto.getCategory());
        habit.setFrequencyType(dto.getFrequencyType());
        habit.setCustomDays(dto.getCustomDays());
        habit.setStartDate(dto.getStartDate() != null ? dto.getStartDate() : checklistService.getUserToday(user));
        habit.setEndDate(dto.getEndDate());
        habit.setActive(dto.isActive());
        habit.setReminderTime(dto.getReminderTime());
        habit.setCreatedAt(LocalDateTime.now());

        habit = habitRepository.save(habit);
        return new HabitDto(habit);
    }

    @Transactional
    public List<HabitDto> createBatchHabits(User user, List<HabitDto> dtos) {
        List<HabitDto> results = new ArrayList<>();
        LocalDate today = checklistService.getUserToday(user);
        for (HabitDto dto : dtos) {
            if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) continue;
            Habit habit = new Habit();
            habit.setUser(user);
            habit.setTitle(dto.getTitle().trim());
            habit.setCategory(dto.getCategory());
            habit.setFrequencyType(dto.getFrequencyType());
            habit.setCustomDays(dto.getCustomDays());
            habit.setStartDate(dto.getStartDate() != null ? dto.getStartDate() : today);
            habit.setEndDate(dto.getEndDate());
            habit.setActive(dto.isActive());
            habit.setReminderTime(dto.getReminderTime());
            habit.setCreatedAt(LocalDateTime.now());

            habit = habitRepository.save(Objects.requireNonNull(habit));
            results.add(new HabitDto(habit));
        }
        return results;
    }

    @Transactional
    public HabitDto updateHabit(User user, Long habitId, HabitDto dto) {
        Habit habit = habitRepository.findById(Objects.requireNonNull(habitId))
                .filter(h -> h.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id: " + habitId));

        habit.setTitle(dto.getTitle().trim());
        habit.setCategory(dto.getCategory());
        habit.setFrequencyType(dto.getFrequencyType());
        habit.setCustomDays(dto.getCustomDays());
        habit.setStartDate(dto.getStartDate());
        habit.setEndDate(dto.getEndDate());
        habit.setActive(dto.isActive());
        habit.setReminderTime(dto.getReminderTime());

        habit = habitRepository.save(habit);
        return new HabitDto(habit);
    }

    @Transactional
    public void deleteHabit(User user, Long habitId) {
        Habit habit = habitRepository.findById(Objects.requireNonNull(habitId))
                .filter(h -> h.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found: " + habitId));
        habitRepository.delete(Objects.requireNonNull(habit));
    }

    @Transactional
    public HabitDto toggleHabitActive(User user, Long habitId) {
        Habit habit = habitRepository.findById(Objects.requireNonNull(habitId))
                .filter(h -> h.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found: " + habitId));

        habit.setActive(!habit.isActive());
        habit = habitRepository.save(habit);
        return new HabitDto(habit);
    }
}
