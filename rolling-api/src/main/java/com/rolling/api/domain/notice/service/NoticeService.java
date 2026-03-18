package com.rolling.api.domain.notice.service;

import com.rolling.api.domain.notice.dto.NoticeCreateRequest;
import com.rolling.api.domain.notice.dto.NoticeListItemResponse;
import com.rolling.api.domain.notice.dto.NoticeResponse;
import com.rolling.api.domain.notice.dto.NoticeUpdateRequest;
import com.rolling.api.domain.notice.entity.Notice;
import com.rolling.api.domain.notice.repository.NoticeRepository;
import com.rolling.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;

    @Transactional(readOnly = true)
    public Page<NoticeListItemResponse> findAll(Pageable pageable) {
        Pageable pageableWithSort = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending());

        return noticeRepository.findAllBy(pageableWithSort)
                .map(NoticeListItemResponse::from);
    }

    @Transactional(readOnly = true)
    public NoticeResponse findById(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("공지사항을 찾을 수 없습니다"));
        return NoticeResponse.from(notice);
    }

    @Transactional
    public NoticeResponse create(NoticeCreateRequest request) {
        String authorName = requireText(request.getAuthorName(), "작성자 표시는 필수입니다");
        String createdBy = normalizeOptionalText(request.getCreatedBy(), "createdBy는 비어 있을 수 없습니다");

        Notice notice = Notice.builder()
                .title(requireText(request.getTitle(), "공지사항 제목은 필수입니다"))
                .content(requireText(request.getContent(), "공지사항 본문은 필수입니다"))
                .authorName(authorName)
                .createdBy(createdBy != null ? createdBy : authorName)
                .build();

        Notice saved = noticeRepository.save(notice);
        return NoticeResponse.from(saved);
    }

    @Transactional
    public NoticeResponse update(Long id, NoticeUpdateRequest request) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("공지사항을 찾을 수 없습니다"));
        validateAtLeastOneField(request);

        notice.update(
                normalizeOptionalText(request.getTitle(), "공지사항 제목은 비어 있을 수 없습니다"),
                normalizeOptionalText(request.getContent(), "공지사항 본문은 비어 있을 수 없습니다"),
                normalizeOptionalText(request.getAuthorName(), "작성자 표시는 비어 있을 수 없습니다"),
                normalizeOptionalText(request.getCreatedBy(), "createdBy는 비어 있을 수 없습니다")
        );

        return NoticeResponse.from(notice);
    }

    @Transactional
    public void delete(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("공지사항을 찾을 수 없습니다"));
        noticeRepository.delete(notice);
    }

    private void validateAtLeastOneField(NoticeUpdateRequest request) {
        if (request.getTitle() == null
                && request.getContent() == null
                && request.getAuthorName() == null
                && request.getCreatedBy() == null) {
            throw BusinessException.badRequest("수정할 필드가 없습니다");
        }
    }

    private String requireText(String value, String message) {
        String normalized = normalizeOptionalText(value, message);
        if (normalized == null) {
            throw BusinessException.badRequest(message);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value, String blankMessage) {
        if (value == null) {
            return null;
        }
        if (!StringUtils.hasText(value)) {
            throw BusinessException.badRequest(blankMessage);
        }
        return value.trim();
    }
}
