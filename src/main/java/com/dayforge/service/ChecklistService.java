package com.dayforge.service;

import com.dayforge.dto.*;
import com.dayforge.entity.*;
import com.dayforge.entity.enums.AuditAction;
import com.dayforge.entity.enums.FrequencyType;
import com.dayforge.exception.BadRequestException;
import com.dayforge.exception.DateSecurityException;
import com.dayforge.exception.ResourceNotFoundException;
import com.dayforge.repository.ChecklistItemRepository;
import com.dayforge.repository.CorrectionAuditRepository;
import com.dayforge.repository.DailyRecordRepository;
import com.dayforge.repository.HabitRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChecklistService {

    private final DailyRecordRepository dailyRecordRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final HabitRepository habitRepository;
    private final CorrectionAuditRepository correctionAuditRepository;
    private final StreakService streakService;
    private final GoalService goalService;

    public ChecklistService(
        DailyRecordRepository dailyRecordRepository,
        ChecklistItemRepository checklistItemRepository,
        HabitRepository habitRepository,
        CorrectionAuditRepository correctionAuditRepository,
        @Lazy StreakService streakService,
        @Lazy GoalService goalService
    ) {
        this.dailyRecordRepository = dailyRecordRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.habitRepository = habitRepository;
        this.correctionAuditRepository = correctionAuditRepository;
        this.streakService = streakService;
        this.goalService = goalService;
    }

    public LocalDate getUserToday(User user) {
        try {
            String tz = (user != null && user.getTimezone() != null && !user.getTimezone().isBlank())
                    ? user.getTimezone()
                    : "Asia/Kolkata";
            return LocalDate.now(ZoneId.of(tz));
        } catch (Exception e) {
            return LocalDate.now(ZoneId.of("Asia/Kolkata"));
        }
    }

    @Transactional
    public DailyChecklistResponse getTodayChecklist(User user) {
        LocalDate today = getUserToday(user);
        return getOrCreateDailyChecklist(user, today);
    }

    @Transactional
    public DailyChecklistResponse getDailyChecklist(User user, LocalDate date) {
        return getOrCreateDailyChecklist(user, date);
    }

    @Transactional
    public DailyChecklistResponse getOrCreateDailyChecklist(User user, LocalDate date) {
        LocalDate today = getUserToday(user);
        DailyRecord record = dailyRecordRepository.findByUserAndRecordDate(user, date)
                .orElseGet(() -> {
                    DailyRecord newRec = new DailyRecord(user, date);
                    return dailyRecordRepository.save(newRec);
                });

        // If newly created or has 0 items, auto-generate from active habits
        if (record.getItems().isEmpty()) {
            generateItemsFromHabits(user, record, date);
            record = dailyRecordRepository.save(record);
        }

        return toDailyResponse(record, today);
    }

    @Transactional
    public void generateItemsFromHabits(User user, DailyRecord record, LocalDate date) {
        // Auto-heal any habits created during the timezone discrepancy window
        List<Habit> activeAll = habitRepository.findByUserAndActiveTrue(user);
        for (Habit h : activeAll) {
            if (h.getStartDate() != null && h.getStartDate().isAfter(date) && !h.getStartDate().isAfter(date.plusDays(1))) {
                h.setStartDate(date);
                habitRepository.save(h);
            }
        }

        List<Habit> activeHabits = habitRepository.findActiveHabitsForDate(user, date);
        int dayOfWeek = date.getDayOfWeek().getValue(); // 1 = Monday, 7 = Sunday
        int orderIndex = 0;

        for (Habit habit : activeHabits) {
            boolean matches = false;
            if (habit.getFrequencyType() == FrequencyType.DAILY) {
                matches = true;
            } else if (habit.getFrequencyType() == FrequencyType.WEEKDAYS) {
                matches = (dayOfWeek <= 5);
            } else if (habit.getFrequencyType() == FrequencyType.CUSTOM_DAYS && habit.getCustomDays() != null) {
                String[] parts = habit.getCustomDays().split(",");
                for (String p : parts) {
                    if (p.trim().equals(String.valueOf(dayOfWeek))) {
                        matches = true;
                        break;
                    }
                }
            }

            if (matches) {
                // Ensure duplicate habit isn't already added
                boolean alreadyPresent = record.getItems().stream()
                        .anyMatch(i -> i.getTitle().equalsIgnoreCase(habit.getTitle().trim()));
                if (!alreadyPresent) {
                    ChecklistItem item = new ChecklistItem(
                        record,
                        user,
                        habit.getTitle().trim(),
                        habit.getCategory(),
                        orderIndex++,
                        habit.getId()
                    );
                    record.getItems().add(item);
                }
            }
        }
        record.recalculateStats();
    }

    @Transactional
    public DailyChecklistResponse createBatchItems(User user, BatchCreateItemsDto dto) {
        LocalDate today = getUserToday(user);
        LocalDate targetDate = (dto.getDate() != null) ? dto.getDate() : today;

        validateDateForModification(today, targetDate, "Cannot add activities to");

        DailyRecord record = dailyRecordRepository.findByUserAndRecordDate(user, targetDate)
                .orElseGet(() -> dailyRecordRepository.save(new DailyRecord(user, targetDate)));

        String[] activities = dto.getActivities().split("[,\\n]+");
        int nextOrder = record.getItems().size();

        for (String act : activities) {
            String title = act.trim();
            if (title.isEmpty()) continue;

            boolean duplicate = record.getItems().stream()
                    .anyMatch(i -> i.getTitle().equalsIgnoreCase(title));
            if (!duplicate) {
                ChecklistItem item = new ChecklistItem(
                    record,
                    user,
                    title,
                    dto.getDefaultCategory(),
                    nextOrder++,
                    null
                );
                record.getItems().add(item);
            }
        }

        record.recalculateStats();
        record = dailyRecordRepository.save(record);
        return toDailyResponse(record, today);
    }

    @Transactional
    public DailyChecklistResponse createSingleItem(User user, CreateItemDto dto) {
        LocalDate today = getUserToday(user);
        LocalDate targetDate = (dto.getDate() != null) ? dto.getDate() : today;

        validateDateForModification(today, targetDate, "Cannot add activities to");

        DailyRecord record = dailyRecordRepository.findByUserAndRecordDate(user, targetDate)
                .orElseGet(() -> dailyRecordRepository.save(new DailyRecord(user, targetDate)));

        String title = dto.getTitle().trim();
        boolean duplicate = record.getItems().stream()
                .anyMatch(i -> i.getTitle().equalsIgnoreCase(title));
        if (duplicate) {
            throw new BadRequestException("An activity with this name already exists for today");
        }

        int nextOrder = record.getItems().size();
        ChecklistItem item = new ChecklistItem(
            record,
            user,
            title,
            dto.getCategory(),
            nextOrder,
            null
        );
        record.getItems().add(item);
        record.recalculateStats();
        record = dailyRecordRepository.save(record);

        return toDailyResponse(record, today);
    }

    @Transactional
    public ChecklistItemDto toggleItemStatus(User user, Long itemId) {
        ChecklistItem item = checklistItemRepository.findByIdAndUser(itemId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found with id: " + itemId));

        LocalDate today = getUserToday(user);
        LocalDate recordDate = item.getDailyRecord().getRecordDate();

        // Strict Date Protection: Upcoming days CANNOT be completed prematurely!
        if (recordDate.isAfter(today)) {
            throw new DateSecurityException("Upcoming-day protection: You cannot complete or modify future checklist items ahead of time.");
        }

        // Past days are read-only to preserve historical data
        if (recordDate.isBefore(today)) {
            throw new DateSecurityException("Date protection: Past checklist records are archived and read-only.");
        }

        int nextAttempt = item.getCorrectionAttempts() + 1;
        item.setCorrectionAttempts(nextAttempt);

        // Toggle completed status
        boolean newStatus = !item.isCompleted();
        item.setCompleted(newStatus);
        item.setCompletedAt(newStatus ? LocalDateTime.now() : null);

        // Audit log action
        AuditAction auditAction = newStatus ? AuditAction.CHECKED : AuditAction.UNCHECKED;
        CorrectionAudit audit = new CorrectionAudit(item, user, recordDate, auditAction, nextAttempt);
        correctionAuditRepository.save(audit);

        item.setUpdatedAt(LocalDateTime.now());
        item = checklistItemRepository.save(item);

        // Update DailyRecord aggregate stats
        DailyRecord record = item.getDailyRecord();
        record.recalculateStats();
        dailyRecordRepository.save(record);

        // Recalculate streak & goals
        streakService.recalculateStreak(user);
        goalService.syncGoalProgress(user);

        return new ChecklistItemDto(item);
    }

    @Transactional
    public ChecklistItemDto updateItem(User user, Long itemId, UpdateItemDto dto) {
        ChecklistItem item = checklistItemRepository.findByIdAndUser(itemId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found: " + itemId));

        LocalDate today = getUserToday(user);
        LocalDate recordDate = item.getDailyRecord().getRecordDate();
        validateDateForModification(today, recordDate, "Cannot edit activities on");

        item.setTitle(dto.getTitle().trim());
        if (dto.getCategory() != null) {
            item.setCategory(dto.getCategory());
        }
        item.setUpdatedAt(LocalDateTime.now());
        item = checklistItemRepository.save(item);

        return new ChecklistItemDto(item);
    }

    @Transactional
    public void deleteItem(User user, Long itemId) {
        ChecklistItem item = checklistItemRepository.findByIdAndUser(itemId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found: " + itemId));

        LocalDate today = getUserToday(user);
        LocalDate recordDate = item.getDailyRecord().getRecordDate();
        validateDateForModification(today, recordDate, "Cannot delete activities from");

        DailyRecord record = item.getDailyRecord();
        record.getItems().remove(item);
        checklistItemRepository.delete(item);

        record.recalculateStats();
        dailyRecordRepository.save(record);

        streakService.recalculateStreak(user);
        goalService.syncGoalProgress(user);
    }

    @Transactional
    public DailyChecklistResponse reorderItem(User user, Long itemId, int direction) {
        // direction: -1 = move up, +1 = move down
        ChecklistItem targetItem = checklistItemRepository.findByIdAndUser(itemId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found: " + itemId));

        DailyRecord record = targetItem.getDailyRecord();
        List<ChecklistItem> items = record.getItems();

        // Sort by orderIndex then ID to get a stable ordering consistent with display
        List<ChecklistItem> sorted = items.stream()
                .sorted(Comparator.comparingInt((ChecklistItem i) -> i.getOrderIndex()).thenComparing(i -> i.getId()))
                .collect(Collectors.toList());

        int currentIndex = -1;
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getId().equals(targetItem.getId())) {
                currentIndex = i;
                break;
            }
        }

        int swapIndex = currentIndex + direction;
        if (currentIndex >= 0 && swapIndex >= 0 && swapIndex < sorted.size()) {
            ChecklistItem otherItem = sorted.get(swapIndex);
            int tempOrder = targetItem.getOrderIndex();
            targetItem.setOrderIndex(otherItem.getOrderIndex());
            otherItem.setOrderIndex(tempOrder);

            checklistItemRepository.save(targetItem);
            checklistItemRepository.save(otherItem);
        }

        LocalDate today = getUserToday(user);
        return toDailyResponse(record, today);
    }

    private void validateDateForModification(LocalDate today, LocalDate targetDate, String actionPrefix) {
        if (targetDate.isAfter(today)) {
            throw new DateSecurityException(actionPrefix + " upcoming days ahead of time.");
        }
        if (targetDate.isBefore(today)) {
            throw new DateSecurityException(actionPrefix + " past historical records.");
        }
    }

    private DailyChecklistResponse toDailyResponse(DailyRecord record, LocalDate today) {
        DailyChecklistResponse res = new DailyChecklistResponse();
        res.setDate(record.getRecordDate());
        res.setTotalTasks(record.getTotalTasks());
        res.setCompletedTasks(record.getCompletedTasks());
        res.setCompletionPercentage(record.getCompletionPercentage());
        res.setSuccessful(record.isSuccessful());

        res.setPast(record.getRecordDate().isBefore(today));
        res.setToday(record.getRecordDate().isEqual(today));
        res.setFuture(record.getRecordDate().isAfter(today));

        List<ChecklistItemDto> itemDtos = record.getItems().stream()
                .sorted(Comparator.comparingInt((ChecklistItem i) -> i.getOrderIndex()).thenComparing(i -> i.getId()))
                .map(ChecklistItemDto::new)
                .collect(Collectors.toList());
        res.setItems(itemDtos);

        return res;
    }
}
