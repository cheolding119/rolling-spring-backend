package com.rolling.api.domain.traininglog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rolling.api.domain.traininglog.dto.TrainingLogAttendanceGrassDay;
import com.rolling.api.domain.traininglog.dto.TrainingLogAttendanceGrassResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogCategoryInsight;
import com.rolling.api.domain.traininglog.dto.TrainingLogChecklistItem;
import com.rolling.api.domain.traininglog.dto.TrainingLogDailyInsight;
import com.rolling.api.domain.traininglog.dto.TrainingLogHashtagInsight;
import com.rolling.api.domain.traininglog.dto.TrainingLogInsightPeriod;
import com.rolling.api.domain.traininglog.dto.TrainingLogInsightResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogInsightSummary;
import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import com.rolling.api.domain.traininglog.entity.TrainingLogEntry;
import com.rolling.api.domain.traininglog.repository.TrainingLogEntryRepository;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingLogInsightService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<TrainingLogChecklistItem>> CHECKLIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> HASHTAG_TYPE = new TypeReference<>() {
    };
    private static final int ATTENDANCE_GRASS_DAYS = 365;
    private static final int RECENT_ATTENDANCE_DAYS = 30;
    private static final int TOP_HASHTAG_LIMIT = 5;

    private final TrainingLogEntryRepository trainingLogEntryRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public TrainingLogAttendanceGrassResponse getAttendanceGrass(Long userId, LocalDate referenceDate) {
        requireActiveUserExists(userId);
        LocalDate endDate = resolveReferenceDate(referenceDate);
        LocalDate startDate = endDate.minusDays(ATTENDANCE_GRASS_DAYS - 1L);

        Map<LocalDate, List<TrainingLogEntry>> entriesByDate = trainingLogEntryRepository
                .findAllByUser_IdAndTrainingDateGreaterThanEqualAndTrainingDateLessThanOrderByTrainingDateAscCreatedAtAsc(
                        userId,
                        startDate,
                        endDate.plusDays(1)
                )
                .stream()
                .collect(Collectors.groupingBy(
                        TrainingLogEntry::getTrainingDate,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<TrainingLogAttendanceGrassDay> days = new ArrayList<>(ATTENDANCE_GRASS_DAYS);
        for (int i = 0; i < ATTENDANCE_GRASS_DAYS; i++) {
            LocalDate date = startDate.plusDays(i);
            days.add(toAttendanceGrassDay(date, entriesByDate.getOrDefault(date, List.of())));
        }

        int attendanceDays = countAttendanceDays(days);
        return TrainingLogAttendanceGrassResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalDays(ATTENDANCE_GRASS_DAYS)
                .attendanceDays(attendanceDays)
                .currentStreakDays(calculateCurrentStreak(days))
                .longestStreakDays(calculateLongestStreak(days))
                .recent30DaysAttendanceDays(countRecentAttendanceDays(days))
                .days(days)
                .build();
    }

    @Transactional(readOnly = true)
    public TrainingLogInsightResponse getInsights(
            Long userId,
            TrainingLogInsightPeriod period,
            LocalDate referenceDate
    ) {
        requireActiveUserExists(userId);
        TrainingLogInsightPeriod requiredPeriod = requireInsightPeriod(period);
        LocalDate startDate = calculateInsightStartDate(requiredPeriod, referenceDate);
        LocalDate endDate = calculateInsightEndDate(requiredPeriod, referenceDate);

        List<TrainingLogEntry> entries = trainingLogEntryRepository
                .findAllByUser_IdAndTrainingDateGreaterThanEqualAndTrainingDateLessThanOrderByTrainingDateAscCreatedAtAsc(
                        userId,
                        startDate,
                        endDate.plusDays(1)
                );

        List<TrainingLogDailyInsight> dailyStats = createDailyStats(startDate, endDate, entries);

        return TrainingLogInsightResponse.builder()
                .period(requiredPeriod)
                .startDate(startDate)
                .endDate(endDate)
                .summary(createSummary(startDate, endDate, entries, dailyStats))
                .dailyStats(dailyStats)
                .categoryBreakdown(createCategoryBreakdown(entries))
                .topHashtags(createTopHashtags(entries))
                .build();
    }

    LocalDate calculateInsightStartDate(TrainingLogInsightPeriod period, LocalDate referenceDate) {
        TrainingLogInsightPeriod requiredPeriod = requireInsightPeriod(period);
        LocalDate requiredReferenceDate = resolveReferenceDate(referenceDate);
        return switch (requiredPeriod) {
            case WEEK -> requiredReferenceDate.minusDays(requiredReferenceDate.getDayOfWeek().getValue() - 1L);
            case MONTH -> YearMonth.from(requiredReferenceDate).atDay(1);
        };
    }

    LocalDate calculateInsightEndDate(TrainingLogInsightPeriod period, LocalDate referenceDate) {
        TrainingLogInsightPeriod requiredPeriod = requireInsightPeriod(period);
        LocalDate requiredReferenceDate = resolveReferenceDate(referenceDate);
        return switch (requiredPeriod) {
            case WEEK -> calculateInsightStartDate(requiredPeriod, requiredReferenceDate).plusDays(6);
            case MONTH -> YearMonth.from(requiredReferenceDate).atEndOfMonth();
        };
    }

    private TrainingLogAttendanceGrassDay toAttendanceGrassDay(LocalDate date, List<TrainingLogEntry> entries) {
        boolean attended = entries.stream().anyMatch(entry -> Boolean.TRUE.equals(entry.getGymAttendance()));
        int totalTrainingMinutes = entries.stream()
                .map(TrainingLogEntry::getTrainingMinutes)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        List<TrainingLogCategory> categories = entries.stream()
                .map(TrainingLogEntry::getCategory)
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf
                ));

        return new TrainingLogAttendanceGrassDay(
                date,
                toDayOfWeekCode(date.getDayOfWeek()),
                attended,
                calculateAttendanceLevel(attended, totalTrainingMinutes),
                entries.size(),
                totalTrainingMinutes,
                averageInteger(entries.stream()
                        .map(TrainingLogEntry::getTrainingIntensity)
                        .filter(Objects::nonNull)
                        .toList()),
                averageInteger(entries.stream()
                        .map(TrainingLogEntry::getCondition)
                        .filter(Objects::nonNull)
                        .toList()),
                categories
        );
    }

    private List<TrainingLogDailyInsight> createDailyStats(
            LocalDate startDate,
            LocalDate endDate,
            List<TrainingLogEntry> entries
    ) {
        Map<LocalDate, List<TrainingLogEntry>> entriesByDate = entries.stream()
                .collect(Collectors.groupingBy(
                        TrainingLogEntry::getTrainingDate,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        int days = Math.toIntExact(ChronoUnit.DAYS.between(startDate, endDate) + 1);
        List<TrainingLogDailyInsight> dailyStats = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            List<TrainingLogEntry> dailyEntries = entriesByDate.getOrDefault(date, List.of());
            dailyStats.add(toDailyInsight(date, dailyEntries));
        }
        return dailyStats;
    }

    private TrainingLogDailyInsight toDailyInsight(LocalDate date, List<TrainingLogEntry> entries) {
        boolean gymAttendance = entries.stream().anyMatch(entry -> Boolean.TRUE.equals(entry.getGymAttendance()));
        int totalTrainingMinutes = entries.stream()
                .map(TrainingLogEntry::getTrainingMinutes)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        List<TrainingLogCategory> categories = entries.stream()
                .map(TrainingLogEntry::getCategory)
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf
                ));

        return new TrainingLogDailyInsight(
                date,
                entries.size(),
                gymAttendance,
                totalTrainingMinutes,
                averageInteger(entries.stream()
                        .map(TrainingLogEntry::getTrainingIntensity)
                        .filter(Objects::nonNull)
                        .toList()),
                averageInteger(entries.stream()
                        .map(TrainingLogEntry::getCondition)
                        .filter(Objects::nonNull)
                        .toList()),
                categories
        );
    }

    private TrainingLogInsightSummary createSummary(
            LocalDate startDate,
            LocalDate endDate,
            List<TrainingLogEntry> entries,
            List<TrainingLogDailyInsight> dailyStats
    ) {
        int periodDays = Math.toIntExact(ChronoUnit.DAYS.between(startDate, endDate) + 1);
        int trainingDays = (int) dailyStats.stream()
                .filter(day -> day.recordCount() > 0)
                .count();
        int attendanceDays = (int) dailyStats.stream()
                .filter(day -> Boolean.TRUE.equals(day.gymAttendance()))
                .count();
        int totalTrainingMinutes = entries.stream()
                .map(TrainingLogEntry::getTrainingMinutes)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        return TrainingLogInsightSummary.builder()
                .recordCount(entries.size())
                .trainingDays(trainingDays)
                .attendanceDays(attendanceDays)
                .attendanceRate(roundOneDecimal(attendanceDays * 100.0 / periodDays))
                .totalTrainingMinutes(totalTrainingMinutes)
                .averageTrainingMinutesPerAttendanceDay(attendanceDays == 0
                        ? null
                        : roundOneDecimal(totalTrainingMinutes * 1.0 / attendanceDays))
                .averageTrainingIntensity(averageInteger(entries.stream()
                        .map(TrainingLogEntry::getTrainingIntensity)
                        .filter(Objects::nonNull)
                        .toList()))
                .averageCondition(averageInteger(entries.stream()
                        .map(TrainingLogEntry::getCondition)
                        .filter(Objects::nonNull)
                        .toList()))
                .checklistCompletionRate(calculateChecklistCompletionRate(entries))
                .build();
    }

    private List<TrainingLogCategoryInsight> createCategoryBreakdown(List<TrainingLogEntry> entries) {
        Map<TrainingLogCategory, CategoryAccumulator> categoryAccumulators = new LinkedHashMap<>();
        for (TrainingLogEntry entry : entries) {
            TrainingLogCategory category = entry.getCategory();
            if (category == null) {
                continue;
            }
            CategoryAccumulator accumulator = categoryAccumulators.computeIfAbsent(
                    category,
                    ignored -> new CategoryAccumulator()
            );
            accumulator.recordCount++;
            if (entry.getTrainingMinutes() != null) {
                accumulator.totalTrainingMinutes += entry.getTrainingMinutes();
            }
        }

        return categoryAccumulators.entrySet().stream()
                .map(entry -> new TrainingLogCategoryInsight(
                        entry.getKey(),
                        entry.getValue().recordCount,
                        entry.getValue().totalTrainingMinutes
                ))
                .toList();
    }

    private List<TrainingLogHashtagInsight> createTopHashtags(List<TrainingLogEntry> entries) {
        Map<String, Integer> hashtagCounts = new LinkedHashMap<>();
        for (TrainingLogEntry entry : entries) {
            for (String hashtag : readHashtags(entry.getHashtagsJson())) {
                hashtagCounts.merge(hashtag, 1, Integer::sum);
            }
        }

        return hashtagCounts.entrySet().stream()
                .sorted((left, right) -> Integer.compare(right.getValue(), left.getValue()))
                .limit(TOP_HASHTAG_LIMIT)
                .map(entry -> new TrainingLogHashtagInsight(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Double calculateChecklistCompletionRate(List<TrainingLogEntry> entries) {
        int totalChecklistCount = 0;
        int completedChecklistCount = 0;

        for (TrainingLogEntry entry : entries) {
            List<TrainingLogChecklistItem> checklist = readChecklist(entry.getChecklistJson());
            totalChecklistCount += checklist.size();
            completedChecklistCount += (int) checklist.stream()
                    .filter(TrainingLogChecklistItem::checked)
                    .count();
        }

        if (totalChecklistCount == 0) {
            return null;
        }
        return roundOneDecimal(completedChecklistCount * 100.0 / totalChecklistCount);
    }

    private LocalDate resolveReferenceDate(LocalDate referenceDate) {
        return referenceDate != null ? referenceDate : LocalDate.now(clock);
    }

    private int calculateAttendanceLevel(boolean attended, int totalTrainingMinutes) {
        if (!attended) {
            return 0;
        }
        if (totalTrainingMinutes >= 120) {
            return 3;
        }
        if (totalTrainingMinutes >= 60) {
            return 2;
        }
        return 1;
    }

    private int countAttendanceDays(List<TrainingLogAttendanceGrassDay> days) {
        return (int) days.stream()
                .filter(day -> Boolean.TRUE.equals(day.attended()))
                .count();
    }

    private int calculateCurrentStreak(List<TrainingLogAttendanceGrassDay> days) {
        int streak = 0;
        for (int i = days.size() - 1; i >= 0; i--) {
            if (!Boolean.TRUE.equals(days.get(i).attended())) {
                break;
            }
            streak++;
        }
        return streak;
    }

    private int calculateLongestStreak(List<TrainingLogAttendanceGrassDay> days) {
        int current = 0;
        int longest = 0;
        for (TrainingLogAttendanceGrassDay day : days) {
            if (Boolean.TRUE.equals(day.attended())) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 0;
            }
        }
        return longest;
    }

    private int countRecentAttendanceDays(List<TrainingLogAttendanceGrassDay> days) {
        int fromIndex = Math.max(0, days.size() - RECENT_ATTENDANCE_DAYS);
        return countAttendanceDays(days.subList(fromIndex, days.size()));
    }

    private Double averageInteger(List<Integer> values) {
        if (values.isEmpty()) {
            return null;
        }
        double average = values.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElseThrow();
        return roundOneDecimal(average);
    }

    private Double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private List<TrainingLogChecklistItem> readChecklist(String checklistJson) {
        return readJsonList(checklistJson, CHECKLIST_TYPE);
    }

    private List<String> readHashtags(String hashtagsJson) {
        return readJsonList(hashtagsJson, HASHTAG_TYPE);
    }

    private <T> List<T> readJsonList(String json, TypeReference<List<T>> typeReference) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<T> value = OBJECT_MAPPER.readValue(json, typeReference);
            return value == null ? List.of() : List.copyOf(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("훈련 기록 JSON 역직렬화에 실패했습니다", e);
        }
    }

    private String toDayOfWeekCode(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "MON";
            case TUESDAY -> "TUE";
            case WEDNESDAY -> "WED";
            case THURSDAY -> "THU";
            case FRIDAY -> "FRI";
            case SATURDAY -> "SAT";
            case SUNDAY -> "SUN";
        };
    }

    private TrainingLogInsightPeriod requireInsightPeriod(TrainingLogInsightPeriod period) {
        if (period == null) {
            throw BusinessException.badRequest("period는 필수입니다");
        }
        return period;
    }

    private void requireActiveUserExists(Long userId) {
        if (!userRepository.existsByIdAndIsWithdrawnFalse(userId)) {
            throw BusinessException.notFound("사용자를 찾을 수 없습니다");
        }
    }

    private static class CategoryAccumulator {

        private int recordCount;
        private int totalTrainingMinutes;
    }
}
