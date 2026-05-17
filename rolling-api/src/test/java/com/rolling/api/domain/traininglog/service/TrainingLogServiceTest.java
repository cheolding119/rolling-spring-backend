package com.rolling.api.domain.traininglog.service;

import com.rolling.api.domain.traininglog.dto.TrainingLogCalendarSummaryResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogChecklistItemRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntryCreateRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntryResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntryUpdateRequest;
import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import com.rolling.api.domain.traininglog.entity.TrainingLogEntry;
import com.rolling.api.domain.traininglog.repository.TrainingLogCalendarDailyProjection;
import com.rolling.api.domain.traininglog.repository.TrainingLogCalendarMonthlyProjection;
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
    }

    @Test
    @DisplayName("create normalizes checklist hashtags and optional image URL")
    void create_normalizesChecklistHashtagsAndImageUrl() {
        User user = createUser(10L);
        TrainingLogEntryCreateRequest request = new TrainingLogEntryCreateRequest();
        ReflectionTestUtils.setField(request, "category", TrainingLogCategory.TECHNIQUE);
        ReflectionTestUtils.setField(request, "title", "  Arm Triangle Details  ");
        ReflectionTestUtils.setField(request, "content", "  Finished details for knee angle and arm position.  ");
        ReflectionTestUtils.setField(request, "trainingMinutes", 90);
        ReflectionTestUtils.setField(request, "imageUrl", "  https://cdn.test.com/training-log.jpg  ");
        ReflectionTestUtils.setField(request, "checklist", List.of(
                checklistItem("  Triangle Review  ", true),
                checklistItem("Live Roll Application", null)
        ));
        ReflectionTestUtils.setField(request, "hashtags", List.of(" Triangle ", "#Arm-Triangle", "triangle", " "));

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
                .isEqualTo("[{\"text\":\"Triangle Review\",\"checked\":true},{\"text\":\"Live Roll Application\",\"checked\":false}]");
        assertThat(saved.getHashtagsJson()).isEqualTo("[\"triangle\",\"arm-triangle\"]");
        assertThat(saved.getImageUrl()).isEqualTo("https://cdn.test.com/training-log.jpg");
        assertThat(saved.getTrainingMinutes()).isEqualTo(90);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getChecklist()).hasSize(2);
        assertThat(response.getChecklist().get(0).text()).isEqualTo("Triangle Review");
        assertThat(response.getHashtags()).containsExactly("triangle", "arm-triangle");
        assertThat(response.getImageUrl()).isEqualTo("https://cdn.test.com/training-log.jpg");
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

        given(userRepository.findByIdAndIsWithdrawnFalse(10L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> trainingLogService.create(10L, LocalDate.of(2026, 5, 17), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("beltColor는 PROMOTION 카테고리에서만 사용할 수 있습니다");
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
    @DisplayName("update can clear checklist image and training minutes while normalizing hashtags")
    void update_clearsOptionalFieldsAndNormalizesHashtags() {
        User user = createUser(10L);
        TrainingLogEntry entry = createEntry(1L, user, LocalDate.of(2026, 5, 17));
        entry.update(
                TrainingLogCategory.TECHNIQUE,
                "Original Title",
                "Original Content",
                "[{\"text\":\"Existing Checklist\",\"checked\":true}]",
                "[\"triangle\"]",
                60,
                "https://cdn.test.com/original.jpg",
                null,
                null,
                null
        );
        given(trainingLogEntryRepository.findById(1L)).willReturn(Optional.of(entry));

        TrainingLogEntryUpdateRequest request = new TrainingLogEntryUpdateRequest();
        request.setChecklist(List.of());
        request.setHashtags(List.of(" Guard-Pass ", "guard-pass", "#Back-Take"));
        request.setImageUrl(null);
        request.setTrainingMinutes(null);
        ReflectionTestUtils.setField(request, "title", "  Updated Title  ");
        ReflectionTestUtils.setField(request, "content", "  Updated Content  ");

        TrainingLogEntryResponse response = trainingLogService.update(10L, 1L, request);

        assertThat(entry.getTitle()).isEqualTo("Updated Title");
        assertThat(entry.getContent()).isEqualTo("Updated Content");
        assertThat(entry.getChecklistJson()).isNull();
        assertThat(entry.getHashtagsJson()).isEqualTo("[\"guard-pass\",\"back-take\"]");
        assertThat(entry.getImageUrl()).isNull();
        assertThat(entry.getTrainingMinutes()).isNull();

        assertThat(response.getChecklist()).isEmpty();
        assertThat(response.getHashtags()).containsExactly("guard-pass", "back-take");
        assertThat(response.getImageUrl()).isNull();
        assertThat(response.getTrainingMinutes()).isNull();
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
                30,
                null,
                null,
                BeltColor.BLUE,
                2
        );
        given(trainingLogEntryRepository.findById(1L)).willReturn(Optional.of(entry));

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
    @DisplayName("calendar summary aggregates daily monthly totals and active days")
    void getCalendarSummary_aggregatesDailyAndMonthlyValues() {
        given(userRepository.existsByIdAndIsWithdrawnFalse(10L)).willReturn(true);
        given(trainingLogEntryRepository.findDailySummariesByUserIdAndTrainingDateBetween(
                10L,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1)
        )).willReturn(List.of(
                new TrainingLogCalendarDailyProjection(LocalDate.of(2026, 5, 1), 150L, 2L),
                new TrainingLogCalendarDailyProjection(LocalDate.of(2026, 5, 3), 0L, 1L),
                new TrainingLogCalendarDailyProjection(LocalDate.of(2026, 6, 1), 30L, 1L)
        ));
        given(trainingLogEntryRepository.findMonthlySummariesByUserIdAndTrainingDateBetween(
                10L,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1)
        )).willReturn(List.of(
                new TrainingLogCalendarMonthlyProjection(5, 150L, 2L),
                new TrainingLogCalendarMonthlyProjection(6, 30L, 1L)
        ));

        TrainingLogCalendarSummaryResponse response = trainingLogService.getCalendarSummary(10L, 2026);

        assertThat(response.getYear()).isEqualTo(2026);
        assertThat(response.getTotalTrainingMinutes()).isEqualTo(180);
        assertThat(response.getActiveDays()).isEqualTo(3);
        assertThat(response.getMonthlySummaries()).hasSize(2);
        assertThat(response.getMonthlySummaries().get(0).month()).isEqualTo(5);
        assertThat(response.getMonthlySummaries().get(0).totalMinutes()).isEqualTo(150);
        assertThat(response.getMonthlySummaries().get(0).activeDays()).isEqualTo(2);
        assertThat(response.getDailySummaries()).hasSize(3);
        assertThat(response.getDailySummaries().get(1).totalMinutes()).isZero();
        assertThat(response.getDailySummaries().get(1).recordCount()).isEqualTo(1);
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
        TrainingLogEntry entry = createEntry(1L, createUser(10L), LocalDate.of(2026, 5, 17));
        given(trainingLogEntryRepository.findById(1L)).willReturn(Optional.of(entry));

        trainingLogService.delete(10L, 1L);

        verify(trainingLogEntryRepository).delete(entry);
    }

    private TrainingLogChecklistItemRequest checklistItem(String text, Boolean checked) {
        TrainingLogChecklistItemRequest item = new TrainingLogChecklistItemRequest();
        ReflectionTestUtils.setField(item, "text", text);
        ReflectionTestUtils.setField(item, "checked", checked);
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
}
