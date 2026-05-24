package com.rolling.api.domain.traininglog.service;

import com.rolling.api.domain.traininglog.dto.TrainingLogExternalLink;
import com.rolling.api.domain.traininglog.dto.TrainingLogExternalLinkRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogChecklistItem;
import com.rolling.api.domain.traininglog.dto.TrainingLogChecklistItemRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntryCreateRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntryResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntrySummaryResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntryUpdateRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogMonthlyCalendarDailySummary;
import com.rolling.api.domain.traininglog.dto.TrainingLogMonthlyCalendarResponse;
import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import com.rolling.api.domain.traininglog.entity.TrainingLogEntry;
import com.rolling.api.domain.traininglog.entity.TrainingLogColor;
import com.rolling.api.domain.traininglog.entity.TrainingLogLinkType;
import com.rolling.api.domain.traininglog.entity.TrainingLogVisibility;
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

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingLogService {

    private static final int MAX_CHECKLIST_ITEMS = 20;
    private static final int MAX_HASHTAGS = 10;
    private static final int MAX_EXTERNAL_LINKS = 3;
    private static final int MAX_IMAGE_URLS = 10;
    private static final int TAG_AUTOCOMPLETE_LIMIT = 20;
    private static final int RECENT_LIMIT = 10;
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("^[0-9a-z가-힣-]+$");
    private static final Map<TrainingLogLinkType, Set<String>> ALLOWED_EXTERNAL_LINK_HOSTS = Map.of(
            TrainingLogLinkType.INSTAGRAM, Set.of("instagram.com"),
            TrainingLogLinkType.YOUTUBE, Set.of("youtube.com", "youtu.be")
    );

    private final TrainingLogEntryRepository trainingLogEntryRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<TrainingLogEntrySummaryResponse> findEntrySummaries(Long userId, LocalDate trainingDate) {
        requireActiveUserExists(userId);
        return trainingLogEntryRepository.findAllByUser_IdAndTrainingDateOrderByCreatedAtAsc(userId, trainingDate).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TrainingLogMonthlyCalendarResponse getMonthlyCalendarSummary(Long userId, int year, int month) {
        requireActiveUserExists(userId);
        validateYear(year);
        validateMonth(month);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDateExclusive = startDate.plusMonths(1);

        List<TrainingLogMonthlyCalendarDailySummary> dailySummaries = buildMonthlyDailySummaries(
                trainingLogEntryRepository.findAllByUser_IdAndTrainingDateGreaterThanEqualAndTrainingDateLessThanOrderByTrainingDateAscCreatedAtAsc(
                        userId,
                        startDate,
                        endDateExclusive
                )
        );

        return TrainingLogMonthlyCalendarResponse.builder()
                .year(year)
                .month(month)
                .dailySummaries(dailySummaries)
                .build();
    }

    @Transactional(readOnly = true)
    public TrainingLogEntryResponse findEntryDetail(Long userId, Long entryId) {
        TrainingLogEntry entry = getEntry(entryId);
        validateOwner(entry, userId);
        return toResponse(entry);
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
        List<TrainingLogExternalLink> externalLinks = normalizeExternalLinks(request.getExternalLinks());
        List<String> imageUrls = resolveImageUrls(request.getImageUrls(), request.getImageUrl());
        String imageUrl = firstImageUrl(imageUrls);
        Integer trainingIntensity = validateTrainingIntensity(request.getTrainingIntensity());
        Integer condition = validateCondition(request.getCondition());
        Integer trainingMinutes = request.getTrainingMinutes();
        TrainingLogVisibility visibility = normalizeVisibility(request.getVisibility());
        validateCategoryFields(category, request.getBeltColor(), request.getStripeCount());

        TrainingLogEntry saved = trainingLogEntryRepository.save(
                TrainingLogEntry.builder()
                        .user(user)
                        .trainingDate(trainingDate)
                        .category(category)
                        .title(requireText(request.getTitle(), "기록 제목은 필수입니다"))
                        .content(requireText(request.getContent(), "기록 내용은 필수입니다"))
                        .checklistJson(TrainingLogJsonCodec.writeChecklistJson(checklist))
                        .hashtagsJson(TrainingLogJsonCodec.writeStringListJson(hashtags))
                        .imageUrlsJson(TrainingLogJsonCodec.writeStringListJson(imageUrls))
                        .externalLinksJson(TrainingLogJsonCodec.writeExternalLinksJson(externalLinks))
                        .color(request.getColor())
                        .trainingIntensity(trainingIntensity)
                        .gymAttendance(request.getGymAttendance())
                        .condition(condition)
                        .trainingMinutes(trainingMinutes)
                        .imageUrl(imageUrl)
                        .visibility(visibility)
                        .beltColor(category == TrainingLogCategory.PROMOTION ? request.getBeltColor() : null)
                        .stripeCount(category == TrainingLogCategory.PROMOTION ? request.getStripeCount() : null)
                        .build()
        );

        if (category == TrainingLogCategory.PROMOTION) {
            syncUserBeltColor(userId);
        }
        return toResponse(saved);
    }

    @Transactional
    public TrainingLogEntryResponse update(Long userId, Long entryId, TrainingLogEntryUpdateRequest request) {
        TrainingLogEntry entry = getEntry(entryId);
        validateOwner(entry, userId);
        TrainingLogCategory previousCategory = entry.getCategory();

        TrainingLogCategory category = request.getCategory() != null ? request.getCategory() : entry.getCategory();
        String title = request.getTitle() != null
                ? requireText(request.getTitle(), "기록 제목은 필수입니다")
                : entry.getTitle();
        String content = request.getContent() != null
                ? requireText(request.getContent(), "기록 내용은 필수입니다")
                : entry.getContent();
        List<TrainingLogChecklistItem> checklist = request.hasChecklistField()
                ? normalizeChecklist(request.getChecklist())
                : TrainingLogJsonCodec.readChecklist(entry.getChecklistJson());
        List<String> hashtags = request.hasHashtagsField()
                ? normalizeHashtags(request.getHashtags())
                : TrainingLogJsonCodec.readStringList(entry.getHashtagsJson());
        List<TrainingLogExternalLink> externalLinks = request.hasExternalLinksField()
                ? normalizeExternalLinks(request.getExternalLinks())
                : TrainingLogJsonCodec.readExternalLinks(entry.getExternalLinksJson());
        List<String> imageUrls = resolveImageUrls(entry, request);
        String imageUrl = firstImageUrl(imageUrls);
        TrainingLogColor color = request.hasColorField() ? request.getColor() : entry.getColor();
        TrainingLogVisibility visibility = request.hasVisibilityField()
                ? normalizeVisibility(request.getVisibility())
                : entry.getVisibility();
        Integer trainingIntensity = request.hasTrainingIntensityField()
                ? validateTrainingIntensity(request.getTrainingIntensity())
                : entry.getTrainingIntensity();
        Boolean gymAttendance = request.hasGymAttendanceField()
                ? request.getGymAttendance()
                : entry.getGymAttendance();
        Integer condition = request.hasConditionField()
                ? validateCondition(request.getCondition())
                : entry.getCondition();
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
                TrainingLogJsonCodec.writeChecklistJson(checklist),
                TrainingLogJsonCodec.writeStringListJson(hashtags),
                trainingMinutes,
                trainingIntensity,
                gymAttendance,
                condition,
                imageUrl,
                TrainingLogJsonCodec.writeStringListJson(imageUrls),
                TrainingLogJsonCodec.writeExternalLinksJson(externalLinks),
                color,
                visibility,
                beltColor,
                stripeCount
        );

        if (previousCategory == TrainingLogCategory.PROMOTION || category == TrainingLogCategory.PROMOTION) {
            syncUserBeltColor(userId);
        }
        return toResponse(entry);
    }

    @Transactional
    public void delete(Long userId, Long entryId) {
        TrainingLogEntry entry = getEntry(entryId);
        validateOwner(entry, userId);
        trainingLogEntryRepository.delete(entry);
        if (entry.getCategory() == TrainingLogCategory.PROMOTION) {
            syncUserBeltColor(userId);
        }
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
            for (String hashtag : TrainingLogJsonCodec.readStringList(hashtagsJson)) {
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
                .color(entry.getColor())
                .visibility(entry.getVisibility())
                .trainingIntensity(entry.getTrainingIntensity())
                .gymAttendance(entry.getGymAttendance())
                .condition(entry.getCondition())
                .title(entry.getTitle())
                .content(entry.getContent())
                .checklist(TrainingLogJsonCodec.readChecklist(entry.getChecklistJson()))
                .hashtags(TrainingLogJsonCodec.readStringList(entry.getHashtagsJson()))
                .externalLinks(TrainingLogJsonCodec.readExternalLinks(entry.getExternalLinksJson()))
                .imageUrls(readImageUrls(entry.getImageUrlsJson(), entry.getImageUrl()))
                .imageUrl(entry.getImageUrl())
                .trainingMinutes(entry.getTrainingMinutes())
                .beltColor(entry.getBeltColor())
                .stripeCount(entry.getStripeCount())
                .build();
    }

    private TrainingLogEntrySummaryResponse toSummaryResponse(TrainingLogEntry entry) {
        return TrainingLogEntrySummaryResponse.builder()
                .id(entry.getId())
                .title(entry.getTitle())
                .content(entry.getContent())
                .category(entry.getCategory())
                .color(entry.getColor())
                .createdAt(entry.getCreatedAt())
                .build();
    }

    private List<TrainingLogMonthlyCalendarDailySummary> buildMonthlyDailySummaries(List<TrainingLogEntry> entries) {
        if (entries.isEmpty()) {
            return List.of();
        }

        Map<LocalDate, List<TrainingLogEntry>> groupedEntries = entries.stream()
                .collect(Collectors.groupingBy(
                        TrainingLogEntry::getTrainingDate,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return groupedEntries.entrySet().stream()
                .map(entry -> {
                    List<TrainingLogEntry> dayEntries = entry.getValue();
                    List<TrainingLogColor> colors = dayEntries.stream()
                            .map(TrainingLogEntry::getColor)
                            .filter(Objects::nonNull)
                            .collect(Collectors.collectingAndThen(
                                    Collectors.toCollection(LinkedHashSet::new),
                                    List::copyOf
                            ));
                    List<TrainingLogCategory> categories = dayEntries.stream()
                            .map(TrainingLogEntry::getCategory)
                            .filter(Objects::nonNull)
                            .collect(Collectors.collectingAndThen(
                                    Collectors.toCollection(LinkedHashSet::new),
                                    List::copyOf
                            ));
                    return new TrainingLogMonthlyCalendarDailySummary(
                            entry.getKey(),
                            colors,
                            categories,
                            dayEntries.size()
                    );
                })
                .toList();
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

    private void validateMonth(int month) {
        if (month < 1 || month > 12) {
            throw BusinessException.badRequest("month는 1 이상 12 이하이어야 합니다");
        }
    }

    private Integer validateTrainingIntensity(Integer trainingIntensity) {
        if (trainingIntensity == null) {
            return null;
        }
        if (trainingIntensity < 1 || trainingIntensity > 5) {
            throw BusinessException.badRequest("훈련 강도는 1 이상 5 이하이어야 합니다");
        }
        return trainingIntensity;
    }

    private Integer validateCondition(Integer condition) {
        if (condition == null) {
            return null;
        }
        if (condition < 1 || condition > 5) {
            throw BusinessException.badRequest("컨디션은 1 이상 5 이하이어야 합니다");
        }
        return condition;
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

    private List<String> resolveImageUrls(List<String> imageUrls, String imageUrl) {
        if (imageUrls != null) {
            return normalizeImageUrls(imageUrls);
        }
        if (StringUtils.hasText(imageUrl)) {
            return List.of(normalizeOptionalText(imageUrl));
        }
        return List.of();
    }

    private List<String> resolveImageUrls(TrainingLogEntry entry, TrainingLogEntryUpdateRequest request) {
        if (request.hasImageUrlsField()) {
            return normalizeImageUrls(request.getImageUrls());
        }
        if (request.hasImageUrlField()) {
            return resolveImageUrls(null, request.getImageUrl());
        }
        return readImageUrls(entry.getImageUrlsJson(), entry.getImageUrl());
    }

    private List<String> normalizeImageUrls(List<String> imageUrls) {
        if (imageUrls == null) {
            return List.of();
        }
        if (imageUrls.size() > MAX_IMAGE_URLS) {
            throw BusinessException.badRequest("이미지는 최대 10장까지 입력할 수 있습니다");
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String imageUrl : imageUrls) {
            String normalizedValue = normalizeOptionalText(imageUrl);
            if (normalizedValue != null) {
                normalized.add(normalizedValue);
            }
        }
        return List.copyOf(normalized);
    }

    private String firstImageUrl(List<String> imageUrls) {
        return imageUrls.isEmpty() ? null : imageUrls.get(0);
    }

    private List<TrainingLogExternalLink> normalizeExternalLinks(List<TrainingLogExternalLinkRequest> externalLinks) {
        if (externalLinks == null) {
            return List.of();
        }
        if (externalLinks.size() > MAX_EXTERNAL_LINKS) {
            throw BusinessException.badRequest("외부 링크는 최대 3개까지 입력할 수 있습니다");
        }
        return externalLinks.stream()
                .map(this::normalizeExternalLink)
                .toList();
    }

    private TrainingLogExternalLink normalizeExternalLink(TrainingLogExternalLinkRequest request) {
        if (request == null) {
            throw BusinessException.badRequest("외부 링크는 필수입니다");
        }
        if (request.getType() == null) {
            throw BusinessException.badRequest("외부 링크 타입은 필수입니다");
        }

        String normalizedUrl = normalizeExternalLinkUrl(request.getType(), request.getUrl());
        return new TrainingLogExternalLink(request.getType(), normalizedUrl);
    }

    private String normalizeExternalLinkUrl(TrainingLogLinkType type, String url) {
        String normalized = requireText(url, "외부 링크 URL은 필수입니다");
        if (!normalized.contains("://")) {
            normalized = "https://" + normalized;
        }

        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("외부 링크 URL이 올바르지 않습니다");
        }

        String host = uri.getHost();
        if (!StringUtils.hasText(host)) {
            throw BusinessException.badRequest("외부 링크 URL이 올바르지 않습니다");
        }

        String lowerHost = host.toLowerCase(Locale.ROOT);
        if (!isAllowedExternalLinkHost(type, lowerHost)) {
            throw BusinessException.badRequest("허용된 외부 링크 도메인만 사용할 수 있습니다");
        }

        try {
            return new URI(
                    "https",
                    uri.getUserInfo(),
                    lowerHost,
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment()
            ).toString();
        } catch (URISyntaxException e) {
            throw BusinessException.badRequest("외부 링크 URL이 올바르지 않습니다");
        }
    }

    private boolean isAllowedExternalLinkHost(TrainingLogLinkType type, String host) {
        String normalizedHost = host.startsWith("www.") ? host.substring(4) : host;
        return ALLOWED_EXTERNAL_LINK_HOSTS.getOrDefault(type, Set.of()).contains(normalizedHost);
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
                Boolean.TRUE.equals(item.getChecked()),
                Boolean.TRUE.equals(item.getFavorite()),
                normalizeOptionalText(item.getEmoji())
        );
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

    private List<String> readImageUrls(String imageUrlsJson, String imageUrl) {
        List<String> imageUrls = TrainingLogJsonCodec.readStringList(imageUrlsJson);
        if (!imageUrls.isEmpty()) {
            return imageUrls;
        }
        if (StringUtils.hasText(imageUrl)) {
            return List.of(imageUrl);
        }
        return List.of();
    }

    private void syncUserBeltColor(Long userId) {
        User user = getActiveUser(userId);
        trainingLogEntryRepository
                .findFirstByUser_IdAndCategoryOrderByTrainingDateDescCreatedAtDescIdDesc(userId, TrainingLogCategory.PROMOTION)
                .map(TrainingLogEntry::getBeltColor)
                .ifPresent(user::updateBeltColor);
    }

    private TrainingLogVisibility normalizeVisibility(TrainingLogVisibility visibility) {
        return visibility == null ? TrainingLogVisibility.PRIVATE : visibility;
    }

    private int safeToInt(Long value) {
        return value == null ? 0 : Math.toIntExact(value);
    }
}
