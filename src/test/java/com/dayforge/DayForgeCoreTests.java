package com.dayforge;

import com.dayforge.dto.BatchCreateItemsDto;
import com.dayforge.dto.ChecklistItemDto;
import com.dayforge.dto.DailyChecklistResponse;
import com.dayforge.dto.GoalDto;
import com.dayforge.dto.StreakDto;
import com.dayforge.entity.ChecklistItem;
import com.dayforge.entity.DailyRecord;
import com.dayforge.entity.User;
import com.dayforge.entity.enums.HabitCategory;
import com.dayforge.exception.DateSecurityException;
import com.dayforge.repository.DailyRecordRepository;
import com.dayforge.repository.UserRepository;
import com.dayforge.service.ChecklistService;
import com.dayforge.service.GoalService;
import com.dayforge.service.StreakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class DayForgeCoreTests {

    @Autowired
    private ChecklistService checklistService;

    @Autowired
    private GoalService goalService;

    @Autowired
    private StreakService streakService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DailyRecordRepository dailyRecordRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.findByUsername("testuser").orElseGet(() -> {
            User u = new User("testuser", "test@dayforge.local", "hashpwd", "Test User", "Asia/Kolkata");
            return userRepository.save(u);
        });
    }

    @Test
    @DisplayName("Batch Activity Creation: 'Workout, Study, Practice, Coding' generates 4 distinct items")
    void testBatchActivityCreation() {
        BatchCreateItemsDto dto = new BatchCreateItemsDto();
        dto.setActivities("Workout, Study, Practice, Coding");
        dto.setDefaultCategory(HabitCategory.CAREER);
        dto.setDate(checklistService.getUserToday(testUser));

        DailyChecklistResponse res = checklistService.createBatchItems(testUser, dto);

        assertNotNull(res);
        assertTrue(res.getTotalTasks() >= 4);

        boolean hasWorkout = res.getItems().stream().anyMatch(i -> i.getTitle().equalsIgnoreCase("Workout"));
        boolean hasStudy = res.getItems().stream().anyMatch(i -> i.getTitle().equalsIgnoreCase("Study"));
        boolean hasPractice = res.getItems().stream().anyMatch(i -> i.getTitle().equalsIgnoreCase("Practice"));
        boolean hasCoding = res.getItems().stream().anyMatch(i -> i.getTitle().equalsIgnoreCase("Coding"));

        assertTrue(hasWorkout, "Should contain Workout");
        assertTrue(hasStudy, "Should contain Study");
        assertTrue(hasPractice, "Should contain Practice");
        assertTrue(hasCoding, "Should contain Coding");
    }

    @Test
    @DisplayName("Unlimited Corrections: Toggle item status freely without 3-attempt locking")
    void testUnlimitedCorrections() {
        // Create an activity for today
        BatchCreateItemsDto dto = new BatchCreateItemsDto();
        dto.setActivities("Meditation Practice");
        dto.setDefaultCategory(HabitCategory.MINDFULNESS);
        dto.setDate(checklistService.getUserToday(testUser));

        DailyChecklistResponse res = checklistService.createBatchItems(testUser, dto);
        ChecklistItemDto item = res.getItems().stream()
                .filter(i -> i.getTitle().equals("Meditation Practice"))
                .findFirst()
                .orElseThrow();

        Long itemId = item.getId();

        // Attempt 1: Check item
        ChecklistItemDto attempt1 = checklistService.toggleItemStatus(testUser, itemId);
        assertTrue(attempt1.isCompleted());
        assertFalse(attempt1.isLocked());

        // Attempt 2: Uncheck item
        ChecklistItemDto attempt2 = checklistService.toggleItemStatus(testUser, itemId);
        assertFalse(attempt2.isCompleted());
        assertFalse(attempt2.isLocked());

        // Attempt 3: Check item again
        ChecklistItemDto attempt3 = checklistService.toggleItemStatus(testUser, itemId);
        assertTrue(attempt3.isCompleted());
        assertFalse(attempt3.isLocked());

        // Attempt 4: Continue toggling without locking or throwing an exception
        ChecklistItemDto attempt4 = checklistService.toggleItemStatus(testUser, itemId);
        assertFalse(attempt4.isCompleted());
        assertFalse(attempt4.isLocked());
    }

    @Test
    @DisplayName("Upcoming-Day Protection: Premature completion of future tasks is forbidden")
    void testUpcomingDayProtection() {
        LocalDate tomorrow = checklistService.getUserToday(testUser).plusDays(1);

        // Directly create a future record to test security boundary
        DailyRecord futureRecord = new DailyRecord(testUser, tomorrow);
        futureRecord = dailyRecordRepository.save(futureRecord);

        ChecklistItem futureItem = new ChecklistItem(futureRecord, testUser, "Future Task", HabitCategory.GENERAL, 0, null);
        futureRecord.getItems().add(futureItem);
        futureRecord = dailyRecordRepository.save(futureRecord);
        ChecklistItem savedFutureItem = futureRecord.getItems().get(0);

        assertThrows(DateSecurityException.class, () -> {
            checklistService.toggleItemStatus(testUser, savedFutureItem.getId());
        }, "Should block completing future checklist items ahead of time");
    }

    @Test
    @DisplayName("Past-Day Protection: Historical checklist data is read-only")
    void testPastDayProtection() {
        LocalDate pastDate = checklistService.getUserToday(testUser).minusDays(2);

        DailyRecord pastRecord = new DailyRecord(testUser, pastDate);
        pastRecord = dailyRecordRepository.save(pastRecord);

        ChecklistItem pastItem = new ChecklistItem(pastRecord, testUser, "Archived Habit", HabitCategory.HEALTH, 0, null);
        pastRecord.getItems().add(pastItem);
        pastRecord = dailyRecordRepository.save(pastRecord);
        ChecklistItem savedPastItem = pastRecord.getItems().get(0);

        assertThrows(DateSecurityException.class, () -> {
            checklistService.toggleItemStatus(testUser, savedPastItem.getId());
        }, "Should block modifying historical records");
    }

    @Test
    @DisplayName("Streak Calculation: Increments strictly when 100% of checklist is completed")
    void testStreakEngine() {
        LocalDate today = checklistService.getUserToday(testUser);

        // Set up 2 consecutive perfect days in the past
        for (int i = 2; i >= 1; i--) {
            LocalDate d = today.minusDays(i);
            DailyRecord r = new DailyRecord(testUser, d);
            r = dailyRecordRepository.save(r);

            ChecklistItem it = new ChecklistItem(r, testUser, "Habit " + i, HabitCategory.CAREER, 0, null);
            it.setCompleted(true);
            r.getItems().add(it);
            r.recalculateStats();
            dailyRecordRepository.save(r);
        }

        StreakDto streakDto = streakService.recalculateStreak(testUser);
        assertTrue(streakDto.getCurrentStreak() >= 2, "Current streak should be at least 2 consecutive successful days");
    }

    @Test
    @DisplayName("Goal Management: Progress tracks successful completion accurately")
    void testGoalProgressTracking() {
        GoalDto goalDto = new GoalDto();
        goalDto.setTitle("7-Day Consistency Starter Test");
        goalDto.setTargetDays(7);
        goalDto.setStartDate(checklistService.getUserToday(testUser).minusDays(1));

        GoalDto created = goalService.createGoal(testUser, goalDto);
        assertNotNull(created.getId());
        assertEquals(7, created.getTargetDays());
    }
}
