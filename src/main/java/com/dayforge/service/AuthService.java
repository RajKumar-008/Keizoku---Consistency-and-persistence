package com.dayforge.service;

import com.dayforge.dto.AuthRequest;
import com.dayforge.dto.AuthResponse;
import com.dayforge.dto.RegisterRequest;
import com.dayforge.entity.NotificationPreference;
import com.dayforge.entity.Streak;
import com.dayforge.entity.User;
import com.dayforge.exception.BadRequestException;
import com.dayforge.exception.ResourceNotFoundException;
import com.dayforge.repository.*;
import com.dayforge.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final StreakRepository streakRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final CorrectionAuditRepository correctionAuditRepository;
    private final DailyRecordRepository dailyRecordRepository;
    private final HabitRepository habitRepository;
    private final GoalRepository goalRepository;
    private final AchievementRepository achievementRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthService(
        UserRepository userRepository,
        StreakRepository streakRepository,
        NotificationPreferenceRepository notificationPreferenceRepository,
        ChecklistItemRepository checklistItemRepository,
        CorrectionAuditRepository correctionAuditRepository,
        DailyRecordRepository dailyRecordRepository,
        HabitRepository habitRepository,
        GoalRepository goalRepository,
        AchievementRepository achievementRepository,
        PasswordEncoder passwordEncoder,
        JwtUtil jwtUtil,
        AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.streakRepository = streakRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.correctionAuditRepository = correctionAuditRepository;
        this.dailyRecordRepository = dailyRecordRepository;
        this.habitRepository = habitRepository;
        this.goalRepository = goalRepository;
        this.achievementRepository = achievementRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        User user = new User(
            req.getUsername(),
            req.getEmail(),
            passwordEncoder.encode(req.getPassword()),
            req.getFullName(),
            req.getTimezone()
        );
        user = userRepository.save(user);

        // Initialize default streak record
        Streak streak = new Streak(user);
        streakRepository.save(streak);

        // Initialize default notification preference
        NotificationPreference pref = new NotificationPreference(user);
        notificationPreferenceRepository.save(pref);

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getFullName(), user.getTimezone(), user.getRole());
    }

    public AuthResponse login(AuthRequest req) {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + req.getUsername()));

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getFullName(), user.getTimezone(), user.getRole());
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResourceNotFoundException("No active user session. Please log in.");
        }
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    @Transactional
    public void resetUserData(User user) {
        correctionAuditRepository.deleteByUser(user);
        checklistItemRepository.deleteByUser(user);
        dailyRecordRepository.deleteByUser(user);
        habitRepository.deleteByUser(user);
        goalRepository.deleteByUser(user);
        achievementRepository.deleteByUser(user);

        Streak streak = streakRepository.findByUser(user).orElseGet(() -> new Streak(user));
        streak.setCurrentStreak(0);
        streak.setLongestStreak(0);
        streak.setTotalSuccessfulDays(0);
        streak.setTotalMissedDays(0);
        streak.setLastCalculatedDate(null);
        streak.setUpdatedAt(LocalDateTime.now());
        streakRepository.save(streak);
    }
}
