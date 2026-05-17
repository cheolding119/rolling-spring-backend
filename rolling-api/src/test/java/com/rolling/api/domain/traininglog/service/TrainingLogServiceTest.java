package com.rolling.api.domain.traininglog.service;

import com.rolling.api.domain.traininglog.dto.TrainingLogChecklistItemRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntryCreateRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntryResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntryUpdateRequest;
import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import com.rolling.api.domain.traininglog.entity.TrainingLogEntry;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    @DisplayName("훈련 기록 생성 시 체크리스트와 해시태그를 정규화해 저장한다")
    void create_normalizesChecklistAndHashtags() {
        User user = createUser(10L);
        TrainingLogEntryCreateRequest request = new TrainingLogEntryCreateRequest();
        ReflectionTestUtils.setField(request, "category", TrainingLogCategory.TECHNIQUE);
        ReflectionTestUtils.setField(request, "title", "  암 트라이앵글 디테일  ");
        ReflectionTestUtils.setField(request, "content", "  무릎 각도와 팔 위치를 정리했다.  ");
        ReflectionTestUtils.setField(request, "trainingMinutes", 90);
        ReflectionTestUtils.setField(request, "checklist", List.of(
                checklistItem("  디테일 복습  ", true),
                checklistItem("실전 적용", null)
        ));
        ReflectionTestUtils.setField(request, "hashtags", List.of(" Triangle ", "#Arm-Triangle", "triangle", " "));

        when(userRepository.findByIdAndIsWithdrawnFalse(10L)).thenReturn(Optional.of(user));
        when(trainingLogEntryRepository.save(any(TrainingLogEntry.class))).thenAnswer(invocation -> {
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
        assertThat(saved.getTitle()).isEqualTo("암 트라이앵글 디테일");
        assertThat(saved.getContent()).isEqualTo("무릎 각도와 팔 위치를 정리했다.");
        assertThat(saved.getChecklistJson()).isEqualTo(
                "[{\"text\":\"디테일 복습\",\"checked\":true},{\"text\":\"실전 적용\",\"checked\":false}]"
        );
        assertThat(saved.getHashtagsJson()).isEqualTo("[\"triangle\",\"arm-triangle\"]");
        assertThat(saved.getTrainingMinutes()).isEqualTo(90);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getChecklist()).hasSize(2);
        assertThat(response.getChecklist().get(0).text()).isEqualTo("디테일 복습");
        assertThat(response.getHashtags()).containsExactly("triangle", "arm-triangle");
    }

    @Test
    @DisplayName("미래 날짜 훈련 기록 생성은 거부한다")
    void create_withFutureDate_throwsValidationError() {
        TrainingLogEntryCreateRequest request = new TrainingLogEntryCreateRequest();
        ReflectionTestUtils.setField(request, "category", TrainingLogCategory.TECHNIQUE);
        ReflectionTestUtils.setField(request, "title", "암 트라이앵글");
        ReflectionTestUtils.setField(request, "content", "디테일 정리");

        assertThatThrownBy(() -> trainingLogService.create(10L, LocalDate.of(2026, 5, 18), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("기록 날짜는 미래일 수 없습니다");
    }

    @Test
    @DisplayName("훈련 기록 수정은 본인 기록에만 허용된다")
    void update_whenNotOwner_throwsForbidden() {
        TrainingLogEntry entry = createEntry(1L, createUser(99L), LocalDate.of(2026, 5, 17));
        when(trainingLogEntryRepository.findById(1L)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> trainingLogService.update(10L, 1L, new TrainingLogEntryUpdateRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("본인 기록만 수정/삭제할 수 있습니다");
    }

    @Test
    @DisplayName("훈련 기록 수정 시 체크리스트와 훈련 시간을 비우고 해시태그를 다시 정규화할 수 있다")
    void update_clearsChecklistAndTrainingMinutesAndNormalizesHashtags() {
        User user = createUser(10L);
        TrainingLogEntry entry = createEntry(1L, user, LocalDate.of(2026, 5, 17));
        entry.update(
                TrainingLogCategory.TECHNIQUE,
                "기존 제목",
                "기존 내용",
                "[{\"text\":\"기존 체크\",\"checked\":true}]",
                "[\"triangle\"]",
                60,
                null,
                null,
                null,
                null
        );
        when(trainingLogEntryRepository.findById(1L)).thenReturn(Optional.of(entry));

        TrainingLogEntryUpdateRequest request = new TrainingLogEntryUpdateRequest();
        request.setChecklist(List.of());
        request.setHashtags(List.of(" Guard-Pass ", "guard-pass", "#Back-Take"));
        request.setTrainingMinutes(null);
        ReflectionTestUtils.setField(request, "title", "  수정 제목  ");
        ReflectionTestUtils.setField(request, "content", "  수정 내용  ");

        TrainingLogEntryResponse response = trainingLogService.update(10L, 1L, request);

        assertThat(entry.getTitle()).isEqualTo("수정 제목");
        assertThat(entry.getContent()).isEqualTo("수정 내용");
        assertThat(entry.getChecklistJson()).isNull();
        assertThat(entry.getHashtagsJson()).isEqualTo("[\"guard-pass\",\"back-take\"]");
        assertThat(entry.getTrainingMinutes()).isNull();

        assertThat(response.getChecklist()).isEmpty();
        assertThat(response.getHashtags()).containsExactly("guard-pass", "back-take");
        assertThat(response.getTrainingMinutes()).isNull();
    }

    @Test
    @DisplayName("해시태그 자동완성은 본인 데이터 기준 중복 없는 목록을 최근 순으로 반환한다")
    void autocompleteTags_returnsDistinctMatchesInRecentOrder() {
        when(userRepository.existsByIdAndIsWithdrawnFalse(10L)).thenReturn(true);
        when(trainingLogEntryRepository.findHashtagsJsonByUserId(10L)).thenReturn(List.of(
                "[\"arm-triangle\",\"guard-pass\"]",
                "[\"triangle\",\"armbar\",\"arm-triangle\"]"
        ));

        List<String> response = trainingLogService.autocompleteTags(10L, "tri");

        assertThat(response).containsExactly("arm-triangle", "triangle");
    }

    @Test
    @DisplayName("훈련 기록 삭제는 본인 기록이면 삭제한다")
    void delete_ownedEntry_deletesEntry() {
        TrainingLogEntry entry = createEntry(1L, createUser(10L), LocalDate.of(2026, 5, 17));
        when(trainingLogEntryRepository.findById(1L)).thenReturn(Optional.of(entry));

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
                .title("기존 제목")
                .content("기존 내용")
                .build();
        ReflectionTestUtils.setField(entry, "id", id);
        ReflectionTestUtils.setField(entry, "createdAt", LocalDateTime.of(2026, 5, 17, 9, 0));
        ReflectionTestUtils.setField(entry, "updatedAt", LocalDateTime.of(2026, 5, 17, 9, 0));
        return entry;
    }
}
