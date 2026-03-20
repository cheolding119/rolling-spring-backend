package com.rolling.api.domain.inquiry.service;

import com.rolling.api.domain.inquiry.dto.InquiryAnswerRequest;
import com.rolling.api.domain.inquiry.dto.InquiryCreateRequest;
import com.rolling.api.domain.inquiry.dto.InquiryResponse;
import com.rolling.api.domain.inquiry.dto.InquiryStatusUpdateRequest;
import com.rolling.api.domain.inquiry.entity.Inquiry;
import com.rolling.api.domain.inquiry.entity.InquiryStatus;
import com.rolling.api.domain.inquiry.repository.InquiryRepository;
import com.rolling.api.domain.notification.model.PushNotificationCommand;
import com.rolling.api.domain.notification.model.PushNotificationType;
import com.rolling.api.domain.notification.service.NotificationService;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InquiryService {

    public static final String INQUIRY_DETAIL_ROUTE = "/inquiry/detail";

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    @Transactional
    public InquiryResponse create(Long userId, InquiryCreateRequest request) {
        User user = getActiveUser(userId);

        Inquiry inquiry = Inquiry.builder()
                .user(user)
                .title(requireText(request.getTitle(), "문의 제목은 필수입니다"))
                .content(requireText(request.getContent(), "문의 내용은 필수입니다"))
                .status(InquiryStatus.RECEIVED)
                .build();

        Inquiry saved = inquiryRepository.save(inquiry);
        return InquiryResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<InquiryResponse> findMyInquiries(Long userId, Pageable pageable) {
        requireActiveUserExists(userId);
        return inquiryRepository.findAllByUser_Id(userId, withDefaultSort(pageable))
                .map(InquiryResponse::from);
    }

    @Transactional(readOnly = true)
    public InquiryResponse findMyInquiry(Long userId, Long inquiryId) {
        requireActiveUserExists(userId);
        Inquiry inquiry = inquiryRepository.findByIdAndUser_Id(inquiryId, userId)
                .orElseThrow(() -> BusinessException.notFound("문의를 찾을 수 없습니다"));
        return InquiryResponse.from(inquiry);
    }

    @Transactional(readOnly = true)
    public Page<InquiryResponse> findAllForAdmin(Pageable pageable) {
        return inquiryRepository.findAll(withDefaultSort(pageable))
                .map(InquiryResponse::from);
    }

    @Transactional(readOnly = true)
    public InquiryResponse findByIdForAdmin(Long inquiryId) {
        return InquiryResponse.from(getInquiry(inquiryId));
    }

    @Transactional
    public InquiryResponse answer(Long adminUserId, Long inquiryId, InquiryAnswerRequest request) {
        Inquiry inquiry = getInquiry(inquiryId);
        boolean shouldNotify = inquiry.getStatus() != InquiryStatus.ANSWERED || !inquiry.hasAnswer();

        inquiry.answer(
                requireText(request.getAnswerContent(), "문의 답변은 필수입니다"),
                adminUserId,
                LocalDateTime.now(clock)
        );

        if (shouldNotify) {
            notificationService.saveNotificationsForUsers(
                    List.of(inquiry.getUser().getId()),
                    new PushNotificationCommand(
                            PushNotificationType.INQUIRY_ANSWERED,
                            "문의 답변이 등록되었습니다",
                            "등록한 문의에 운영자 답변이 도착했습니다.",
                            inquiry.getId(),
                            Map.of("route", INQUIRY_DETAIL_ROUTE)
                    )
            );
        }

        return InquiryResponse.from(inquiry);
    }

    @Transactional
    public InquiryResponse updateStatus(Long inquiryId, InquiryStatusUpdateRequest request) {
        Inquiry inquiry = getInquiry(inquiryId);
        InquiryStatus status = request.getStatus();

        if (status == InquiryStatus.ANSWERED && !inquiry.hasAnswer()) {
            throw BusinessException.badRequest("ANSWERED 상태로 변경하려면 답변이 필요합니다");
        }
        if (status != InquiryStatus.ANSWERED && inquiry.hasAnswer()) {
            throw BusinessException.badRequest("답변이 저장된 문의는 ANSWERED 외 상태로 변경할 수 없습니다");
        }

        inquiry.updateStatus(status);
        return InquiryResponse.from(inquiry);
    }

    private Inquiry getInquiry(Long inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> BusinessException.notFound("문의를 찾을 수 없습니다"));
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

    private Pageable withDefaultSort(Pageable pageable) {
        return pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending());
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw BusinessException.badRequest(message);
        }
        return value.trim();
    }
}
