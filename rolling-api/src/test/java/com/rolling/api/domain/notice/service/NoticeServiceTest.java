package com.rolling.api.domain.notice.service;

import com.rolling.api.domain.notice.dto.NoticeCreateRequest;
import com.rolling.api.domain.notice.dto.NoticeListItemResponse;
import com.rolling.api.domain.notice.dto.NoticeResponse;
import com.rolling.api.domain.notice.dto.NoticeUpdateRequest;
import com.rolling.api.domain.notice.entity.Notice;
import com.rolling.api.domain.notice.repository.NoticeRepository;
import com.rolling.api.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    @Test
    @DisplayName("공지사항 목록 조회는 createdAt 내림차순 기본 정렬을 적용하고 응답으로 매핑한다")
    void findAll_appliesDefaultSortAndMapsResponse() {
        Notice notice = notice(10L, "3월 점검 안내", "점검이 예정되어 있습니다.", "Rolling Admin");
        when(noticeRepository.findAllBy(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notice)));

        NoticeService noticeService = new NoticeService(noticeRepository);

        Page<NoticeListItemResponse> response = noticeService.findAll(PageRequest.of(0, 20));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(noticeRepository).findAllBy(pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt").isDescending()).isTrue();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getId()).isEqualTo(10L);
        assertThat(response.getContent().get(0).getContent()).isEqualTo("점검이 예정되어 있습니다.");
        assertThat(response.getContent().get(0).getAuthorName()).isEqualTo("Rolling Admin");
        assertThat(response.getContent().get(0).getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 3, 18, 9, 0));
    }

    @Test
    @DisplayName("공지사항 상세 조회는 단건 응답으로 매핑한다")
    void findById_returnsMappedResponse() {
        Notice notice = notice(20L, "서비스 업데이트", "공지사항 상세 본문", "운영팀");
        when(noticeRepository.findById(20L)).thenReturn(Optional.of(notice));

        NoticeService noticeService = new NoticeService(noticeRepository);

        NoticeResponse response = noticeService.findById(20L);

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getTitle()).isEqualTo("서비스 업데이트");
        assertThat(response.getContent()).isEqualTo("공지사항 상세 본문");
        assertThat(response.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 3, 18, 9, 0));
        assertThat(response.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 3, 18, 10, 30));
    }

    @Test
    @DisplayName("존재하지 않는 공지사항 상세 조회는 NOT_FOUND를 반환한다")
    void findById_whenMissing_throwsNotFound() {
        when(noticeRepository.findById(99L)).thenReturn(Optional.empty());

        NoticeService noticeService = new NoticeService(noticeRepository);

        assertThatThrownBy(() -> noticeService.findById(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("공지사항을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("공지사항 생성 시 createdBy가 비어 있으면 authorName으로 저장한다")
    void create_usesAuthorNameAsFallbackCreatedBy() {
        NoticeCreateRequest request = new NoticeCreateRequest();
        ReflectionTestUtils.setField(request, "title", "3월 서비스 점검 안내");
        ReflectionTestUtils.setField(request, "content", "3월 20일 점검이 진행됩니다.");
        ReflectionTestUtils.setField(request, "authorName", "Rolling Admin");

        when(noticeRepository.save(any(Notice.class))).thenAnswer(invocation -> {
            Notice notice = invocation.getArgument(0);
            ReflectionTestUtils.setField(notice, "id", 30L);
            ReflectionTestUtils.setField(notice, "createdAt", LocalDateTime.of(2026, 3, 18, 12, 0));
            ReflectionTestUtils.setField(notice, "updatedAt", LocalDateTime.of(2026, 3, 18, 12, 0));
            return notice;
        });

        NoticeService noticeService = new NoticeService(noticeRepository);

        NoticeResponse response = noticeService.create(request);

        ArgumentCaptor<Notice> noticeCaptor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeRepository).save(noticeCaptor.capture());
        assertThat(noticeCaptor.getValue().getCreatedBy()).isEqualTo("Rolling Admin");
        assertThat(response.getId()).isEqualTo(30L);
        assertThat(response.getAuthorName()).isEqualTo("Rolling Admin");
    }

    @Test
    @DisplayName("공지사항 수정 시 전달한 필드만 반영한다")
    void update_updatesOnlyProvidedFields() {
        Notice notice = notice(40L, "기존 제목", "기존 본문", "기존 작성자");
        ReflectionTestUtils.setField(notice, "createdBy", "ops-admin");
        NoticeUpdateRequest request = new NoticeUpdateRequest();
        ReflectionTestUtils.setField(request, "title", "변경된 제목");
        ReflectionTestUtils.setField(request, "authorName", "Rolling Team");

        when(noticeRepository.findById(40L)).thenReturn(Optional.of(notice));

        NoticeService noticeService = new NoticeService(noticeRepository);

        NoticeResponse response = noticeService.update(40L, request);

        assertThat(notice.getTitle()).isEqualTo("변경된 제목");
        assertThat(notice.getContent()).isEqualTo("기존 본문");
        assertThat(notice.getAuthorName()).isEqualTo("Rolling Team");
        assertThat(notice.getCreatedBy()).isEqualTo("ops-admin");
        assertThat(response.getTitle()).isEqualTo("변경된 제목");
        assertThat(response.getAuthorName()).isEqualTo("Rolling Team");
    }

    @Test
    @DisplayName("공지사항 수정 시 필드가 하나도 없으면 실패한다")
    void update_whenNoFields_throwsBadRequest() {
        Notice notice = notice(41L, "기존 제목", "기존 본문", "기존 작성자");
        NoticeUpdateRequest request = new NoticeUpdateRequest();

        when(noticeRepository.findById(41L)).thenReturn(Optional.of(notice));

        NoticeService noticeService = new NoticeService(noticeRepository);

        assertThatThrownBy(() -> noticeService.update(41L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("수정할 필드가 없습니다");
    }

    @Test
    @DisplayName("공지사항 삭제는 hard delete 한다")
    void delete_removesNotice() {
        Notice notice = notice(50L, "삭제 대상", "본문", "운영팀");
        when(noticeRepository.findById(50L)).thenReturn(Optional.of(notice));

        NoticeService noticeService = new NoticeService(noticeRepository);

        noticeService.delete(50L);

        verify(noticeRepository).delete(notice);
    }

    private Notice notice(Long id, String title, String content, String authorName) {
        Notice notice = Notice.builder()
                .title(title)
                .content(content)
                .authorName(authorName)
                .createdBy("system-admin")
                .build();
        ReflectionTestUtils.setField(notice, "id", id);
        ReflectionTestUtils.setField(notice, "createdAt", LocalDateTime.of(2026, 3, 18, 9, 0));
        ReflectionTestUtils.setField(notice, "updatedAt", LocalDateTime.of(2026, 3, 18, 10, 30));
        return notice;
    }
}
