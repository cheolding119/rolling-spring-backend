package com.rolling.api.domain.traininglog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rolling.api.domain.traininglog.dto.TrainingLogChecklistItem;
import com.rolling.api.domain.traininglog.dto.TrainingLogChecklistItemRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogCalendarDailySummary;
import com.rolling.api.domain.traininglog.dto.TrainingLogCalendarMonthlySummary;
import com.rolling.api.domain.traininglog.dto.TrainingLogCalendarSummaryResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntryCreateRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntryResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntryUpdateRequest;
import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import com.rolling.api.domain.traininglog.entity.TrainingLogEntry;
import com.rolling.api.domain.traininglog.repository.TrainingLogCalendarDailyProjection;
import com.rolling.api.domain.traininglog.repository.TrainingLogCalendarMonthlyProjection;
import com.rolling.api.domain.traininglog.repository.TrainingLogEntryRepository;
import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import org.springframework.data.domain.PageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TrainingLogService {

    private static final int MAX_CHECKLIST_ITEMS = 20;
    private static final int MAX_HASHTAGS = 10;
    private static final int TAG_AUTOCOMPLETE_LIMIT = 20;
    private static final int RECENT_LIMIT = 10;
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("^[0-9a-z가-힣-]+$");
    private static final TypeReference<List<TrainingLogChecklistItem>> CHECKLIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<String>> HASHTAG_TYPE = new TypeReference<>() {};
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final TrainingLogEntryRepository trainingLogEntryRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<TrainingLogEntryResponse> findEntries(Long userId, LocalDate trainingDate) {
        requireActiveUserExists(userId);
        return trainingLogEntryRepository.findAllByUser_IdAndTrainingDateOrderByCreatedAtAsc(userId, trainingDate).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TrainingLogCalendarSummaryResponse getCalendarSummary(Long userId, int year) {
        requireActiveUserExists(userId);
        validateYear(year);

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDateExclusive = startDate.plusYears(1);

        List<TrainingLogCalendarDailySummary> dailySummaries = trainingLogEntryRepository
                .findDailySummariesByUserIdAndTrainingDateBetween(userId, startDate, endDateExclusive)
                .stream()
                .map(this::toDailySummary)
                .toList();
        List<TrainingLogCalendarMonthlySummary> monthlySummaries = trainingLogEntryRepository
                .findMonthlySummariesByUserIdAndTrainingDateBetween(userId, startDate, endDateExclusive)
                .stream()
                .map(this::toMonthlySummary)
                .toList();

        int totalTrainingMinutes = dailySummaries.stream()
                .map(TrainingLogCalendarDailySummary::totalMinutes)
                .reduce(0, Integer::sum);

        return TrainingLogCalendarSummaryResponse.builder()
                .year(year)
                .totalTrainingMinutes(totalTrainingMinutes)
                .activeDays(dailySummaries.size())
                .monthlySummaries(monthlySummaries)
                .dailySummaries(dailySummaries)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TrainingLogEntryResponse> findRecentEntries(Long userId) {
        requireActiveUserExists(userId);
        return trainingLogEntryRepository
                .findAllByUser_IdOrderByTrainingDateDescCreatedAtDesc(userId, PageRequest.of(0, RECENT_LIMIT))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TrainingLogEntryResponse create(Long userId, LocalDate trainingDate, TrainingLogEntryCreateRequest request) {
        validateTrainingDate(trainingDate);
        User user = getActiveUser(userId);
        TrainingLogCategory category = requireCategory(request.getCategory());
        List<TrainingLogChecklistItem> checklist = normalizeChecklist(request.getChecklist());
        List<String> hashtags = normalizeHashtags(request.getHashtags());
        String imageUrl = normalizeOptionalText(request.getImageUrl());
        validateCategoryFields(category, request.getBeltColor(), request.getStripeCount());

        TrainingLogEntry saved = trainingLogEntryRepository.save(
                TrainingLogEntry.builder()
                        .user(user)
                        .trainingDate(trainingDate)
                        .category(category)
                        .title(requireText(request.getTitle(), "기록 제목은 필수입니다"))
                        .content(requireText(request.getContent(), "기록 내용은 필수입니다"))
                        .checklistJson(writeChecklistJson(checklist))
                        .hashtagsJson(writeHashtagsJson(hashtags))
                        .imageUrl(imageUrl)
                        .trainingMinutes(request.getTrainingMinutes())
                        .beltColor(category == TrainingLogCategory.PROMOTION ? request.getBeltColor() : null)
                        .stripeCount(category == TrainingLogCategory.PROMOTION ? request.getStripeCount() : null)
                        .build()
        );

        return toResponse(saved);
    }

    @Transactional
    public TrainingLogEntryResponse update(Long userId, Long entryId, TrainingLogEntryUpdateRequest request) {
        TrainingLogEntry entry = getEntry(entryId);
        validateOwner(entry, userId);

        TrainingLogCategory category = request.getCategory() != null ? request.getCategory() : entry.getCategory();
        String title = request.getTitle() != null
                ? requireText(request.getTitle(), "기록 제목은 필수입니다")
                : entry.getTitle();
        String content = request.getContent() != null
                ? requireText(request.getContent(), "기록 내용은 필수입니다")
                : entry.getContent();
        List<TrainingLogChecklistItem> checklist = request.hasChecklistField()
                ? normalizeChecklist(request.getChecklist())
                : readChecklist(entry.getChecklistJson());
        List<String> hashtags = request.hasHashtagsField()
                ? normalizeHashtags(request.getHashtags())
                : readHashtags(entry.getHashtagsJson());
        String imageUrl = request.hasImageUrlField()
                ? normalizeOptionalText(request.getImageUrl())
                : entry.getImageUrl();
        Integer trainingMinutes = request.hasTrainingMinutesField()
                ? request.getTrainingMinutes()
                : entry.getTrainingMinutes();
        BeltColor beltColor = resolveBeltColor(entry, request, category);
        Integer stripeCount = resolveStripeCount(entry, request, category);
        validateCategoryFields(category, beltColor, stripeCount);

        entry.update(
                category,
                title,
                content,
                writeChecklistJson(checklist),
                writeHashtagsJson(hashtags),
                trainingMinutes,
                imageUrl,
                entry.getExternalLinksJson(),
                beltColor,
                stripeCount
        );

        return toResponse(entry);
    }

    @Transactional
    public void delete(Long userId, Long entryId) {
        TrainingLogEntry entry = getEntry(entryId);
        validateOwner(entry, userId);
        trainingLogEntryRepository.delete(entry);
    }

    @Transactional(readOnly = true)
    public List<String> autocompleteTags(Long userId, String query) {
        requireActiveUserExists(userId);
        String normalizedQuery = normalizeQuery(query);
        if (normalizedQuery == null) {
            return List.of();
        }

        LinkedHashSet<String> matched = new LinkedHashSet<>();
        for (String hashtagsJson : trainingLogEntryRepository.findHashtagsJsonByUserId(userId)) {
            for (String hashtag : readHashtags(hashtagsJson)) {
                if (hashtag.contains(normalizedQuery)) {
                    matched.add(hashtag);
                    if (matched.size() >= TAG_AUTOCOMPLETE_LIMIT) {
                        return List.copyOf(matched);
                    }
                }
            }
        }
        return List.copyOf(matched);
    }

    private TrainingLogEntryResponse toResponse(TrainingLogEntry entry) {
        return TrainingLogEntryResponse.builder()
                .id(entry.getId())
                .trainingDate(entry.getTrainingDate())
                .category(entry.getCategory())
                .title(entry.getTitle())
                .content(entry.getContent())
                .checklist(readChecklist(entry.getChecklistJson()))
                .hashtags(readHashtags(entry.getHashtagsJson()))
                .imageUrl(entry.getImageUrl())
                .trainingMinutes(entry.getTrainingMinutes())
                .beltColor(entry.getBeltColor())
                .stripeCount(entry.getStripeCount())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }

    private TrainingLogCalendarDailySummary toDailySummary(TrainingLogCalendarDailyProjection projection) {
        return new TrainingLogCalendarDailySummary(
                projection.date(),
                safeToInt(projection.totalMinutes()),
                safeToInt(projection.recordCount())
        );
    }

    private TrainingLogCalendarMonthlySummary toMonthlySummary(TrainingLogCalendarMonthlyProjection projection) {
        return new TrainingLogCalendarMonthlySummary(
                projection.month(),
                safeToInt(projection.totalMinutes()),
                safeToInt(projection.activeDays())
        );
    }

    private TrainingLogEntry getEntry(Long entryId) {
        return trainingLogEntryRepository.findById(entryId)
                .orElseThrow(() -> BusinessException.notFound("훈련 기록을 찾을 수 없습니다"));
    }

    private User getActiveUser(Long userId) {
        return userRepository.findByIdAndIsWithdrawnFalse(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다"));
    }

    private void requireActiveUserExists(Long userId) {
        if (!userRepository.existsByIdAndIsWithdrawnFalse(userId)) {
            throw BusinessException.notFound("사용자를 찾을 수 없습니다");
        }
    }

    private void validateOwner(TrainingLogEntry entry, Long userId) {
        if (!entry.getUser().getId().equals(userId)) {
            throw BusinessException.forbidden("본인 기록만 수정/삭제할 수 있습니다");
        }
    }

    private void validateTrainingDate(LocalDate trainingDate) {
        if (trainingDate.isAfter(LocalDate.now(clock))) {
            throw BusinessException.badRequest("기록 날짜는 미래일 수 없습니다");
        }
    }

    private void validateYear(int year) {
        if (year < 2000 || year > 2100) {
            throw BusinessException.badRequest("year는 2000 이상 2100 이하이어야 합니다");
        }
    }

    private TrainingLogCategory requireCategory(TrainingLogCategory category) {
        if (category == null) {
            throw BusinessException.badRequest("기록 카테고리는 필수입니다");
        }
        return category;
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw BusinessException.badRequest(message);
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private BeltColor resolveBeltColor(
            TrainingLogEntry entry,
            TrainingLogEntryUpdateRequest request,
            TrainingLogCategory category
    ) {
        if (category != TrainingLogCategory.PROMOTION) {
            return null;
        }
        return request.hasBeltColorField() ? request.getBeltColor() : entry.getBeltColor();
    }

    private Integer resolveStripeCount(
            TrainingLogEntry entry,
            TrainingLogEntryUpdateRequest request,
            TrainingLogCategory category
    ) {
        if (category != TrainingLogCategory.PROMOTION) {
            return null;
        }
        return request.hasStripeCountField() ? request.getStripeCount() : entry.getStripeCount();
    }

    private void validateCategoryFields(TrainingLogCategory category, BeltColor beltColor, Integer stripeCount) {
        if (category == TrainingLogCategory.PROMOTION) {
            if (beltColor == null) {
                throw BusinessException.badRequest("PROMOTION 카테고리에서는 beltColor가 필수입니다");
            }
            if (stripeCount != null && stripeCount < 0) {
                throw BusinessException.badRequest("PROMOTION 카테고리의 stripeCount는 0 이상이어야 합니다");
            }
            return;
        }

        if (beltColor != null) {
            throw BusinessException.badRequest("beltColor는 PROMOTION 카테고리에서만 사용할 수 있습니다");
        }
        if (stripeCount != null) {
            throw BusinessException.badRequest("stripeCount는 PROMOTION 카테고리에서만 사용할 수 있습니다");
        }
    }

    private List<TrainingLogChecklistItem> normalizeChecklist(List<TrainingLogChecklistItemRequest> items) {
        if (items == null) {
            return List.of();
        }
        if (items.size() > MAX_CHECKLIST_ITEMS) {
            throw BusinessException.badRequest("체크리스트는 최대 20개까지 입력할 수 있습니다");
        }
        return items.stream()
                .map(this::normalizeChecklistItem)
                .toList();
    }

    private TrainingLogChecklistItem normalizeChecklistItem(TrainingLogChecklistItemRequest item) {
        return new TrainingLogChecklistItem(
                requireText(item.getText(), "체크리스트 항목 내용은 필수입니다"),
                Boolean.TRUE.equals(item.getChecked())
        );
    }

    private String writeChecklistJson(List<TrainingLogChecklistItem> checklist) {
        return writeJsonOrNull(checklist);
    }

    private List<TrainingLogChecklistItem> readChecklist(String checklistJson) {
        return readJsonList(checklistJson, CHECKLIST_TYPE);
    }

    private List<String> normalizeHashtags(List<String> hashtags) {
        if (hashtags == null) {
            return List.of();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String hashtag : hashtags) {
            String normalizedValue = normalizeHashtag(hashtag);
            if (normalizedValue != null) {
                normalized.add(normalizedValue);
            }
        }

        if (normalized.size() > MAX_HASHTAGS) {
            throw BusinessException.badRequest("해시태그는 최대 10개까지 입력할 수 있습니다");
        }
        return List.copyOf(normalized);
    }

    private String normalizeQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        return normalizeHashtag(query);
    }

    private String normalizeHashtag(String hashtag) {
        if (!StringUtils.hasText(hashtag)) {
            return null;
        }

        String normalized = hashtag.trim().toLowerCase(Locale.ROOT);
        while (normalized.startsWith("#")) {
            normalized = normalized.substring(1).trim();
        }

        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (!HASHTAG_PATTERN.matcher(normalized).matches()) {
            throw BusinessException.badRequest("해시태그는 한글, 영문, 숫자, 하이픈만 사용할 수 있습니다");
        }
        return normalized;
    }

    private String writeHashtagsJson(List<String> hashtags) {
        return writeJsonOrNull(hashtags);
    }

    private List<String> readHashtags(String hashtagsJson) {
        return readJsonList(hashtagsJson, HASHTAG_TYPE);
    }

    private String writeJsonOrNull(Object value) {
        if (value instanceof List<?> list && list.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("훈련 기록 JSON 직렬화에 실패했습니다", e);
        }
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

    private int safeToInt(Long value) {
        return value == null ? 0 : Math.toIntExact(value);
    }
}
