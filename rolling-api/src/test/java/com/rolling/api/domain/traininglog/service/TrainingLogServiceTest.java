package com.rolling.api.domain.traininglog.service;

import com.rolling.api.domain.traininglog.dto.TrainingLogChecklistItemRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogExternalLink;
import com.rolling.api.domain.traininglog.dto.TrainingLogExternalLinkRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntryCreateRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntryResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntrySummaryResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntryUpdateRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogMonthlyCalendarDailySummary;
import com.rolling.api.domain.traininglog.dto.TrainingLogMonthlyCalendarResponse;
import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import com.rolling.api.domain.traininglog.entity.TrainingLogColor;
import com.rolling.api.domain.traininglog.entity.TrainingLogEntry;
import com.rolling.api.domain.traininglog.entity.TrainingLogLinkType;
import com.rolling.api.domain.traininglog.repository.TrainingLogEntryRepository;
import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrainingLogServiceTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private TrainingLogEntryRepository trainingLogEntryRepository;

    @Mock
    private UserRepository userRepository;

    private TrainingLogService trainingLogService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-05-17T03:00:00Z"), SEOUL_ZONE);
        trainingLogService = new TrainingLogService(
                trainingLogEntryRepository,
                userRepository,
                fixedClock
        );
        lenient().when(trainingLogEntryRepository.findFirstByUser_IdAndCategoryOrderByTrainingDateDescCreatedAtDescIdDesc(
                any(),
                any(TrainingLogCategory.class)
        )).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("create normalizes checklist hashtags external links and optional image URL")
    void create_normalizesChecklistHashtagsExternalLinksAndImageUrl() {
        User user = createUser(10L);
        TrainingLogEntryCreateRequest request = new TrainingLogEntryCreateRequest();
        ReflectionTestUtils.setField(request, "category", TrainingLogCategory.TECHNIQUE);
        ReflectionTestUtils.setField(request, "title", "  Arm Triangle Details  ");
        ReflectionTestUtils.setField(request, "content", "  Finished details for knee angle and arm position.  ");
        ReflectionTestUtils.setField(request, "imageUrls", List.of(
                "  https://cdn.test.com/training-log-1.jpg  ",
                "https://cdn.test.com/training-log-2.jpg"
        ));
        ReflectionTestUtils.setField(request, "color", TrainingLogColor.BLUE);
        ReflectionTestUtils.setField(request, "trainingIntensity", 4);
        ReflectionTestUtils.setField(request, "gymAttendance", true);
        ReflectionTestUtils.setField(request, "condition", 3);
        ReflectionTestUtils.setField(request, "trainingMinutes", 90);
        ReflectionTestUtils.setField(request, "checklist", List.of(
                checklistItem("  Triangle Review  ", true, true, "🔥"),
                checklistItem("Live Roll Application", null)
        ));
        ReflectionTestUtils.setField(request, "hashtags", List.of(" Triangle ", "#Arm-Triangle", "triangle", " "));
        ReflectionTestUtils.setField(request, "externalLinks", List.of(
                externalLink(TrainingLogLinkType.INSTAGRAM, "  www.instagram.com/p/abc123/  "),
                externalLink(TrainingLogLinkType.YOUTUBE, "http://youtu.be/xyz789")
        ));

        given(userRepository.findByIdAndIsWithdrawnFalse(10L)).willReturn(Optional.of(user));
        given(trainingLogEntryRepository.save(any(TrainingLogEntry.class))).willAnswer(invocation -> {
            TrainingLogEntry entry = invocation.getArgument(0);
            ReflectionTestUtils.setField(entry, "id", 100L);
            ReflectionTestUtils.setField(entry, "createdAt", LocalDateTime.of(2026, 5, 17, 12, 0));
            ReflectionTestUtils.setField(entry, "updatedAt", LocalDateTime.of(2026, 5, 17, 12, 0));
            return entry;
        });

        TrainingLogEntryResponse response = trainingLogService.create(10L, LocalDate.of(2026, 5, 17), request);

        ArgumentCaptor<TrainingLogEntry> captor = ArgumentCaptor.forClass(TrainingLogEntry.class);
        verify(trainingLogEntryRepository).save(captor.capture());
        TrainingLogEntry saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("Arm Triangle Details");
        assertThat(saved.getContent()).isEqualTo("Finished details for knee angle and arm position.");
        assertThat(saved.getChecklistJson())
                .isEqualTo("[{\"text\":\"Triangle Review\",\"checked\":true,\"favorite\":true,\"emoji\":\"🔥\"},{\"text\":\"Live Roll Application\",\"checked\":false,\"favorite\":false}]");
        assertThat(saved.getHashtagsJson()).isEqualTo("[\"triangle\",\"arm-triangle\"]");
        assertThat(saved.getExternalLinksJson()).isEqualTo(
                "[{\"type\":\"INSTAGRAM\",\"url\":\"https://www.instagram.com/p/abc123/\"},{\"type\":\"YOUTUBE\",\"url\":\"https://youtu.be/xyz789\"}]"
        );
        assertThat(saved.getColor()).isEqualTo(TrainingLogColor.BLUE);
        assertThat(saved.getTrainingIntensity()).isEqualTo(4);
        assertThat(saved.getGymAttendance()).isTrue();
        assertThat(saved.getCondition()).isEqualTo(3);
        assertThat(saved.getTrainingMinutes()).isEqualTo(90);
        assertThat(saved.getImageUrl()).isEqualTo("https://cdn.test.com/training-log-1.jpg");
        assertThat(saved.getImageUrlsJson()).isEqualTo(
                "[\"https://cdn.test.com/training-log-1.jpg\",\"https://cdn.test.com/training-log-2.jpg\"]"
        );

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getColor()).isEqualTo(TrainingLogColor.BLUE);
        assertThat(response.getTrainingIntensity()).isEqualTo(4);
        assertThat(response.getGymAttendance()).isTrue();
        assertThat(response.getCondition()).isEqualTo(3);
        assertThat(response.getTrainingMinutes()).isEqualTo(90);
        assertThat(response.getChecklist()).hasSize(2);
        assertThat(response.getChecklist().get(0).text()).isEqualTo("Triangle Review");
        assertThat(response.getHashtags()).containsExactly("triangle", "arm-triangle");
        assertThat(response.getExternalLinks()).extracting(TrainingLogExternalLink::type)
                .containsExactly(TrainingLogLinkType.INSTAGRAM, TrainingLogLinkType.YOUTUBE);
        assertThat(response.getImageUrl()).isEqualTo("https://cdn.test.com/training-log-1.jpg");
        assertThat(response.getImageUrls()).containsExactly(
                "https://cdn.test.com/training-log-1.jpg",
                "https://cdn.test.com/training-log-2.jpg"
        );
    }

    @Test
    @DisplayName("create synchronizes user belt color from latest promotion record")
    void create_promotionSynchronizesUserBeltColor() {
        User user = createUser(10L);
        TrainingLogEntryCreateRequest request = new TrainingLogEntryCreateRequest();
        ReflectionTestUtils.setField(request, "category", TrainingLogCategory.PROMOTION);
        ReflectionTestUtils.setField(request, "title", "Promotion");
        ReflectionTestUtils.setField(request, "content", "Blue belt promotion");
        ReflectionTestUtils.setField(request, "beltColor", BeltColor.BLUE);
        ReflectionTestUtils.setField(request, "stripeCount", 1);
        ReflectionTestUtils.setField(request, "trainingIntensity", 5);
        ReflectionTestUtils.setField(request, "trainingMinutes", 120);

        TrainingLogEntry[] savedHolder = new TrainingLogEntry[1];
        given(userRepository.findByIdAndIsWithdrawnFalse(10L)).willReturn(Optional.of(user));
        given(trainingLogEntryRepository.save(any(TrainingLogEntry.class))).willAnswer(invocation -> {
            TrainingLogEntry entry = invocation.getArgument(0);
            ReflectionTestUtils.setField(entry, "id", 101L);
            ReflectionTestUtils.setField(entry, "createdAt", LocalDateTime.of(2026, 5, 17, 12, 0));
            ReflectionTestUtils.setField(entry, "updatedAt", LocalDateTime.of(2026, 5, 17, 12, 0));
            savedHolder[0] = entry;
            return entry;
        });
        given(trainingLogEntryRepository.findFirstByUser_IdAndCategoryOrderByTrainingDateDescCreatedAtDescIdDesc(10L, TrainingLogCategory.PROMOTION))
                .willAnswer(invocation -> Optional.of(savedHolder[0]));

        TrainingLogEntryResponse response = trainingLogService.create(10L, LocalDate.of(2026, 5, 17), request);

        assertThat(user.getBeltColor()).isEqualTo(BeltColor.BLUE);
        assertThat(response.getBeltColor()).isEqualTo(BeltColor.BLUE);
        assertThat(response.getStripeCount()).isEqualTo(1);
        assertThat(response.getTrainingMinutes()).isEqualTo(120);
    }

    @Test
    @DisplayName("create rejects future training dates")
    void create_withFutureDate_throwsValidationError() {
        TrainingLogEntryCreateRequest request = new TrainingLogEntryCreateRequest();
        ReflectionTestUtils.setField(request, "category", TrainingLogCategory.TECHNIQUE);
        ReflectionTestUtils.setField(request, "title", "Arm Triangle");
        ReflectionTestUtils.setField(request, "content", "Details");

        assertThatThrownBy(() -> trainingLogService.create(10L, LocalDate.of(2026, 5, 18), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("기록 날짜는 미래일 수 없습니다");
    }

    @Test
    @DisplayName("create requires belt color for promotion category")
    void create_promotionWithoutBeltColor_throwsValidationError() {
        User user = createUser(10L);
        TrainingLogEntryCreateRequest request = new TrainingLogEntryCreateRequest();
        ReflectionTestUtils.setField(request, "category", TrainingLogCategory.PROMOTION);
        ReflectionTestUtils.setField(request, "title", "Promotion");
        ReflectionTestUtils.setField(request, "content", "Blue belt promotion");

        given(userRepository.findByIdAndIsWithdrawnFalse(10L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> trainingLogService.create(10L, LocalDate.of(2026, 5, 17), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("PROMOTION 카테고리에서는 beltColor가 필수입니다");
    }

    @Test
    @DisplayName("create rejects promotion-only fields on non-promotion categories")
    void create_nonPromotionWithPromotionFields_throwsValidationError() {
        User user = createUser(10L);
        TrainingLogEntryCreateRequest request = new TrainingLogEntryCreateRequest();
        ReflectionTestUtils.setField(request, "category", TrainingLogCategory.TECHNIQUE);
        ReflectionTestUtils.setField(request, "title", "Technique");
        ReflectionTestUtils.setField(request, "content", "Normal training note");
        ReflectionTestUtils.setField(request, "beltColor", BeltColor.BLUE);
        ReflectionTestUtils.setField(request, "color", TrainingLogColor.RED);
        ReflectionTestUtils.setField(request, "trainingIntensity", 3);

        given(userRepository.findByIdAndIsWithdrawnFalse(10L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> trainingLogService.create(10L, LocalDate.of(2026, 5, 17), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("beltColor는 PROMOTION 카테고리에서만 사용할 수 있습니다");
    }

    @Test
    @DisplayName("create rejects training intensity outside the 1 to 5 range")
    void create_invalidTrainingIntensity_throwsValidationError() {
        User user = createUser(10L);
        TrainingLogEntryCreateRequest request = new TrainingLogEntryCreateRequest();
        ReflectionTestUtils.setField(request, "category", TrainingLogCategory.TECHNIQUE);
        ReflectionTestUtils.setField(request, "title", "Technique");
        ReflectionTestUtils.setField(request, "content", "Normal training note");
        ReflectionTestUtils.setField(request, "trainingIntensity", 6);

        given(userRepository.findByIdAndIsWithdrawnFalse(10L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> trainingLogService.create(10L, LocalDate.of(2026, 5, 17), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("훈련 강도는 1 이상 5 이하이어야 합니다");
    }

    @Test
    @DisplayName("create rejects condition outside the 1 to 5 range")
    void create_invalidCondition_throwsValidationError() {
        User user = createUser(10L);
        TrainingLogEntryCreateRequest request = new TrainingLogEntryCreateRequest();
        ReflectionTestUtils.setField(request, "category", TrainingLogCategory.TECHNIQUE);
        ReflectionTestUtils.setField(request, "title", "Technique");
        ReflectionTestUtils.setField(request, "content", "Normal training note");
        ReflectionTestUtils.setField(request, "condition", 0);

        given(userRepository.findByIdAndIsWithdrawnFalse(10L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> trainingLogService.create(10L, LocalDate.of(2026, 5, 17), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("컨디션은 1 이상 5 이하이어야 합니다");
    }

    @Test
    @DisplayName("update persists training status fields when present")
    void update_setsTrainingStatusFields() {
        User user = createUser(10L);
        TrainingLogEntry entry = createEntry(1L, user, LocalDate.of(2026, 5, 17));
        given(trainingLogEntryRepository.findById(1L)).willReturn(Optional.of(entry));

        TrainingLogEntryUpdateRequest request = new TrainingLogEntryUpdateRequest();
        request.setTrainingIntensity(5);
        request.setGymAttendance(true);
        request.setCondition(2);
        request.setTrainingMinutes(75);

        TrainingLogEntryResponse response = trainingLogService.update(10L, 1L, request);

        assertThat(entry.getTrainingIntensity()).isEqualTo(5);
        assertThat(entry.getGymAttendance()).isTrue();
        assertThat(entry.getCondition()).isEqualTo(2);
        assertThat(entry.getTrainingMinutes()).isEqualTo(75);
        assertThat(response.getTrainingIntensity()).isEqualTo(5);
        assertThat(response.getGymAttendance()).isTrue();
        assertThat(response.getCondition()).isEqualTo(2);
        assertThat(response.getTrainingMinutes()).isEqualTo(75);
    }

    @Test
    @DisplayName("update only allows owners to modify entries")
    void update_whenNotOwner_throwsForbidden() {
        TrainingLogEntry entry = createEntry(1L, createUser(99L), LocalDate.of(2026, 5, 17));
        given(trainingLogEntryRepository.findById(1L)).willReturn(Optional.of(entry));

        assertThatThrownBy(() -> trainingLogService.update(10L, 1L, new TrainingLogEntryUpdateRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("본인 기록만 수정/삭제할 수 있습니다");
    }

    @Test
    @DisplayName("update can clear checklist and images while normalizing hashtags")
    void update_clearsOptionalFieldsAndNormalizesHashtags() {
        User user = createUser(10L);
        TrainingLogEntry entry = createEntry(1L, user, LocalDate.of(2026, 5, 17));
        entry.update(
                TrainingLogCategory.TECHNIQUE,
                "Original Title",
                "Original Content",
                "[{\"text\":\"Existing Checklist\",\"checked\":true}]",
                "[\"triangle\"]",
                null,
                null,
                "https://cdn.test.com/original.jpg",
                "[\"https://cdn.test.com/original.jpg\"]",
                null,
                TrainingLogColor.BLUE,
                null,
                null
        );
        given(trainingLogEntryRepository.findById(1L)).willReturn(Optional.of(entry));

        TrainingLogEntryUpdateRequest request = new TrainingLogEntryUpdateRequest();
        request.setChecklist(List.of());
        request.setHashtags(List.of(" Guard-Pass ", "guard-pass", "#Back-Take"));
        request.setExternalLinks(List.of(
                externalLink(TrainingLogLinkType.INSTAGRAM, "instagram.com/p/guardpass")
        ));
        request.setImageUrls(List.of());
        ReflectionTestUtils.setField(request, "title", "  Updated Title  ");
        ReflectionTestUtils.setField(request, "content", "  Updated Content  ");

        TrainingLogEntryResponse response = trainingLogService.update(10L, 1L, request);

        assertThat(entry.getTitle()).isEqualTo("Updated Title");
        assertThat(entry.getContent()).isEqualTo("Updated Content");
        assertThat(entry.getChecklistJson()).isNull();
        assertThat(entry.getHashtagsJson()).isEqualTo("[\"guard-pass\",\"back-take\"]");
        assertThat(entry.getExternalLinksJson()).isEqualTo(
                "[{\"type\":\"INSTAGRAM\",\"url\":\"https://instagram.com/p/guardpass\"}]"
        );
        assertThat(entry.getImageUrl()).isNull();
        assertThat(entry.getImageUrlsJson()).isNull();

        assertThat(response.getChecklist()).isEmpty();
        assertThat(response.getHashtags()).containsExactly("guard-pass", "back-take");
        assertThat(response.getExternalLinks()).extracting(TrainingLogExternalLink::type)
                .containsExactly(TrainingLogLinkType.INSTAGRAM);
        assertThat(response.getImageUrl()).isNull();
        assertThat(response.getImageUrls()).isEmpty();
        assertThat(response.getColor()).isEqualTo(TrainingLogColor.BLUE);
        assertThat(response.getTrainingIntensity()).isNull();
    }

    @Test
    @DisplayName("update clears promotion fields when category changes away from promotion")
    void update_clearsPromotionFieldsForNonPromotionCategory() {
        User user = createUser(10L);
        TrainingLogEntry entry = createEntry(1L, user, LocalDate.of(2026, 5, 17));
        entry.update(
                TrainingLogCategory.PROMOTION,
                "Promotion Title",
                "Promotion Content",
                null,
                null,
                null,
                null,
                null,
                BeltColor.BLUE,
                2
        );
        given(trainingLogEntryRepository.findById(1L)).willReturn(Optional.of(entry));
        given(userRepository.findByIdAndIsWithdrawnFalse(10L)).willReturn(Optional.of(user));

        TrainingLogEntryUpdateRequest request = new TrainingLogEntryUpdateRequest();
        ReflectionTestUtils.setField(request, "category", TrainingLogCategory.TECHNIQUE);

        TrainingLogEntryResponse response = trainingLogService.update(10L, 1L, request);

        assertThat(entry.getCategory()).isEqualTo(TrainingLogCategory.TECHNIQUE);
        assertThat(entry.getBeltColor()).isNull();
        assertThat(entry.getStripeCount()).isNull();
        assertThat(response.getBeltColor()).isNull();
        assertThat(response.getStripeCount()).isNull();
    }

    @Test
    @DisplayName("update rejects external links with unsupported domains")
    void update_withUnsupportedExternalLinkDomain_throwsValidationError() {
        User user = createUser(10L);
        TrainingLogEntry entry = createEntry(1L, user, LocalDate.of(2026, 5, 17));
        given(trainingLogEntryRepository.findById(1L)).willReturn(Optional.of(entry));

        TrainingLogEntryUpdateRequest request = new TrainingLogEntryUpdateRequest();
        request.setExternalLinks(List.of(
                externalLink(TrainingLogLinkType.INSTAGRAM, "https://youtube.com/watch?v=abc")
        ));

        assertThatThrownBy(() -> trainingLogService.update(10L, 1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("허용된 외부 링크 도메인만 사용할 수 있습니다");
    }

    @Test
    @DisplayName("delete synchronizes user belt color from the latest remaining promotion record")
    void delete_promotionSynchronizesLatestRemainingBeltColor() {
        User user = createUser(10L);
        TrainingLogEntry deletedEntry = createEntry(1L, user, LocalDate.of(2026, 5, 17));
        deletedEntry.update(
                TrainingLogCategory.PROMOTION,
                "Promotion",
                "Blue belt promotion",
                null,
                null,
                null,
                null,
                null,
                BeltColor.BLUE,
                1
        );
        TrainingLogEntry latestPromotion = createEntry(2L, user, LocalDate.of(2026, 5, 10));
        latestPromotion.update(
                TrainingLogCategory.PROMOTION,
                "Previous Promotion",
                "Purple belt promotion",
                null,
                null,
                null,
                null,
                null,
                BeltColor.PURPLE,
                0
        );
        given(trainingLogEntryRepository.findById(1L)).willReturn(Optional.of(deletedEntry));
        given(userRepository.findByIdAndIsWithdrawnFalse(10L)).willReturn(Optional.of(user));
        given(trainingLogEntryRepository.findFirstByUser_IdAndCategoryOrderByTrainingDateDescCreatedAtDescIdDesc(10L, TrainingLogCategory.PROMOTION))
                .willReturn(Optional.of(latestPromotion));

        trainingLogService.delete(10L, 1L);

        verify(trainingLogEntryRepository).delete(deletedEntry);
        assertThat(user.getBeltColor()).isEqualTo(BeltColor.PURPLE);
    }

    @Test
    @DisplayName("monthly calendar groups day colors and totals")
    void getMonthlyCalendarSummary_groupsDayColorsAndTotals() {
        given(userRepository.existsByIdAndIsWithdrawnFalse(10L)).willReturn(true);
        given(trainingLogEntryRepository.findAllByUser_IdAndTrainingDateGreaterThanEqualAndTrainingDateLessThanOrderByTrainingDateAscCreatedAtAsc(
                10L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1)
        )).willReturn(List.of(
                createEntryWithColor(1L, createUser(10L), LocalDate.of(2026, 5, 1), TrainingLogColor.BLUE, 60),
                createEntryWithColor(2L, createUser(10L), LocalDate.of(2026, 5, 1), TrainingLogColor.RED, 30, TrainingLogCategory.PROMOTION),
                createEntryWithColor(3L, createUser(10L), LocalDate.of(2026, 5, 3), TrainingLogColor.GREEN, 0, TrainingLogCategory.TECHNIQUE)
        ));

        TrainingLogMonthlyCalendarResponse response = trainingLogService.getMonthlyCalendarSummary(10L, 2026, 5);

        assertThat(response.getYear()).isEqualTo(2026);
        assertThat(response.getMonth()).isEqualTo(5);
        assertThat(response.getDailySummaries()).hasSize(2);
        assertThat(response.getDailySummaries().get(0).date()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(response.getDailySummaries().get(0).colors()).containsExactly(TrainingLogColor.BLUE, TrainingLogColor.RED);
        assertThat(response.getDailySummaries().get(0).categories()).containsExactly(TrainingLogCategory.TECHNIQUE, TrainingLogCategory.PROMOTION);
        assertThat(response.getDailySummaries().get(0).recordCount()).isEqualTo(2);
        assertThat(response.getDailySummaries().get(1).date()).isEqualTo(LocalDate.of(2026, 5, 3));
        assertThat(response.getDailySummaries().get(1).colors()).containsExactly(TrainingLogColor.GREEN);
        assertThat(response.getDailySummaries().get(1).categories()).containsExactly(TrainingLogCategory.TECHNIQUE);
        assertThat(response.getDailySummaries().get(1).recordCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("date summary returns summary cards for the selected day")
    void findEntrySummaries_returnsSummaryCardsForDate() {
        given(userRepository.existsByIdAndIsWithdrawnFalse(10L)).willReturn(true);
        given(trainingLogEntryRepository.findAllByUser_IdAndTrainingDateOrderByCreatedAtAsc(10L, LocalDate.of(2026, 5, 17)))
                .willReturn(List.of(
                        createEntryWithColor(2L, createUser(10L), LocalDate.of(2026, 5, 17), TrainingLogColor.RED, 30),
                        createEntryWithColor(1L, createUser(10L), LocalDate.of(2026, 5, 17), TrainingLogColor.BLUE, 60)
                ));

        List<TrainingLogEntrySummaryResponse> response = trainingLogService.findEntrySummaries(10L, LocalDate.of(2026, 5, 17));

        assertThat(response).extracting(TrainingLogEntrySummaryResponse::getId).containsExactly(2L, 1L);
        assertThat(response.get(0).getCategory()).isEqualTo(TrainingLogCategory.TECHNIQUE);
        assertThat(response.get(0).getColor()).isEqualTo(TrainingLogColor.RED);
        assertThat(response.get(1).getCategory()).isEqualTo(TrainingLogCategory.TECHNIQUE);
        assertThat(response.get(1).getColor()).isEqualTo(TrainingLogColor.BLUE);
    }

    @Test
    @DisplayName("detail lookup returns the full response")
    void findEntryDetail_returnsFullResponse() {
        User user = createUser(10L);
        TrainingLogEntry entry = createEntry(1L, user, LocalDate.of(2026, 5, 17));
        entry.update(
                TrainingLogCategory.TECHNIQUE,
                "Original Title",
                "Original Content",
                "[{\"text\":\"Existing Checklist\",\"checked\":true,\"favorite\":true,\"emoji\":\"🔥\"}]",
                "[\"triangle\"]",
                "https://cdn.test.com/original.jpg",
                "[\"https://cdn.test.com/original.jpg\"]",
                null,
                TrainingLogColor.BLUE,
                null,
                null
        );
        given(trainingLogEntryRepository.findById(1L)).willReturn(Optional.of(entry));

        TrainingLogEntryResponse response = trainingLogService.findEntryDetail(10L, 1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getColor()).isEqualTo(TrainingLogColor.BLUE);
        assertThat(response.getChecklist()).hasSize(1);
        assertThat(response.getChecklist().get(0).favorite()).isTrue();
        assertThat(response.getImageUrls()).containsExactly("https://cdn.test.com/original.jpg");
    }

    @Test
    @DisplayName("recent entries use the fixed page size and preserve repository ordering")
    void findRecentEntries_usesFixedPageSize() {
        given(userRepository.existsByIdAndIsWithdrawnFalse(10L)).willReturn(true);
        given(trainingLogEntryRepository.findAllByUser_IdOrderByTrainingDateDescCreatedAtDesc(any(), any(Pageable.class)))
                .willReturn(List.of(
                        createEntry(2L, createUser(10L), LocalDate.of(2026, 5, 17)),
                        createEntry(1L, createUser(10L), LocalDate.of(2026, 5, 16))
                ));

        List<TrainingLogEntryResponse> response = trainingLogService.findRecentEntries(10L);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(trainingLogEntryRepository)
                .findAllByUser_IdOrderByTrainingDateDescCreatedAtDesc(org.mockito.ArgumentMatchers.eq(10L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        assertThat(response).extracting(TrainingLogEntryResponse::getId).containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("autocomplete returns distinct matches in repository order")
    void autocompleteTags_returnsDistinctMatchesInRepositoryOrder() {
        given(userRepository.existsByIdAndIsWithdrawnFalse(10L)).willReturn(true);
        given(trainingLogEntryRepository.findHashtagsJsonByUserId(10L)).willReturn(List.of(
                "[\"arm-triangle\",\"guard-pass\"]",
                "[\"triangle\",\"armbar\",\"arm-triangle\"]"
        ));

        List<String> response = trainingLogService.autocompleteTags(10L, "tri");

        assertThat(response).containsExactly("arm-triangle", "triangle");
    }

    @Test
    @DisplayName("delete removes owned entries")
    void delete_ownedEntry_deletesEntry() {
        User user = createUser(10L);
        TrainingLogEntry entry = createEntry(1L, user, LocalDate.of(2026, 5, 17));
        given(trainingLogEntryRepository.findById(1L)).willReturn(Optional.of(entry));

        trainingLogService.delete(10L, 1L);

        verify(trainingLogEntryRepository).delete(entry);
    }

    private TrainingLogChecklistItemRequest checklistItem(String text, Boolean checked) {
        return checklistItem(text, checked, null, null);
    }

    private TrainingLogChecklistItemRequest checklistItem(String text, Boolean checked, Boolean favorite, String emoji) {
        TrainingLogChecklistItemRequest item = new TrainingLogChecklistItemRequest();
        ReflectionTestUtils.setField(item, "text", text);
        ReflectionTestUtils.setField(item, "checked", checked);
        ReflectionTestUtils.setField(item, "favorite", favorite);
        ReflectionTestUtils.setField(item, "emoji", emoji);
        return item;
    }

    private TrainingLogExternalLinkRequest externalLink(TrainingLogLinkType type, String url) {
        TrainingLogExternalLinkRequest item = new TrainingLogExternalLinkRequest();
        ReflectionTestUtils.setField(item, "type", type);
        ReflectionTestUtils.setField(item, "url", url);
        return item;
    }

    private User createUser(Long id) {
        User user = User.builder()
                .socialId("user-" + id)
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("user-" + id)
                .email("user-" + id + "@test.com")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private TrainingLogEntry createEntry(Long id, User user, LocalDate trainingDate) {
        TrainingLogEntry entry = TrainingLogEntry.builder()
                .user(user)
                .trainingDate(trainingDate)
                .category(TrainingLogCategory.TECHNIQUE)
                .title("Existing Title")
                .content("Existing Content")
                .build();
        ReflectionTestUtils.setField(entry, "id", id);
        ReflectionTestUtils.setField(entry, "createdAt", LocalDateTime.of(2026, 5, 17, 9, 0));
        ReflectionTestUtils.setField(entry, "updatedAt", LocalDateTime.of(2026, 5, 17, 9, 0));
        return entry;
    }

    private TrainingLogEntry createEntryWithColor(Long id, User user, LocalDate trainingDate, TrainingLogColor color, Integer trainingMinutes) {
        return createEntryWithColor(id, user, trainingDate, color, trainingMinutes, TrainingLogCategory.TECHNIQUE);
    }

    private TrainingLogEntry createEntryWithColor(Long id, User user, LocalDate trainingDate, TrainingLogColor color, Integer trainingMinutes, TrainingLogCategory category) {
        TrainingLogEntry entry = TrainingLogEntry.builder()
                .user(user)
                .trainingDate(trainingDate)
                .category(category)
                .title("Training " + trainingDate)
                .content("Training note " + trainingDate)
                .color(color)
                .trainingMinutes(trainingMinutes)
                .build();
        ReflectionTestUtils.setField(entry, "id", id);
        ReflectionTestUtils.setField(entry, "createdAt", LocalDateTime.of(2026, 5, 17, 9, 0));
        ReflectionTestUtils.setField(entry, "updatedAt", LocalDateTime.of(2026, 5, 17, 9, 0));
        return entry;
    }
}
