package com.rolling.api.domain.seminar.service;

import com.rolling.api.domain.openmat.entity.Region;
import com.rolling.api.domain.seminar.dto.SeminarApplicationResponse;
import com.rolling.api.domain.seminar.dto.SeminarCancelApplicationRequest;
import com.rolling.api.domain.seminar.dto.SeminarCreateRequest;
import com.rolling.api.domain.seminar.dto.SeminarHostCancelApplicationRequest;
import com.rolling.api.domain.seminar.dto.SeminarResponse;
import com.rolling.api.domain.seminar.dto.SeminarStatusUpdateRequest;
import com.rolling.api.domain.seminar.dto.SeminarUpdateRequest;
import com.rolling.api.domain.seminar.event.SeminarAppliedEvent;
import com.rolling.api.domain.seminar.event.SeminarApplicationCanceledByHostEvent;
import com.rolling.api.domain.seminar.event.SeminarApplicationCanceledEvent;
import com.rolling.api.domain.seminar.event.SeminarCanceledEvent;
import com.rolling.api.domain.seminar.event.SeminarDeletedEvent;
import com.rolling.api.domain.seminar.event.SeminarUpdatedEvent;
import com.rolling.api.domain.seminar.entity.Seminar;
import com.rolling.api.domain.seminar.entity.SeminarApplication;
import com.rolling.api.domain.seminar.entity.SeminarApplicationStatus;
import com.rolling.api.domain.seminar.entity.SeminarStatus;
import com.rolling.api.domain.report.entity.ReportReason;
import com.rolling.api.domain.report.entity.ReportTargetType;
import com.rolling.api.domain.report.repository.ReportRepository;
import com.rolling.api.domain.report.service.ReportService;
import com.rolling.api.domain.seminar.repository.SeminarApplicationRepository;
import com.rolling.api.domain.seminar.repository.SeminarRepository;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeminarService {

    private final SeminarRepository seminarRepository;
    private final SeminarApplicationRepository seminarApplicationRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final ReportService reportService;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public SeminarResponse create(Long hostId, SeminarCreateRequest request) {
        User host = userRepository.findByIdAndIsWithdrawnFalse(hostId)
                .orElseThrow(() -> BusinessException.notFound("호스트 사용자를 찾을 수 없습니다"));

        validateDateRange(request.getStartDateTime(), request.getEndDateTime());
        validateApplicationPeriod(
                request.getApplicationStartDateTime(),
                request.getApplicationEndDateTime(),
                request.getStartDateTime()
        );
        validateCapacity(request.getMaxCapacity());
        validatePrice(request.getPrice());
        validateCreateCoordinates(request);

        Seminar seminar = Seminar.builder()
                .host(host)
                .title(request.getTitle())
                .description(request.getDescription())
                .mainImageUrl(request.getMainImageUrl())
                .instructorName(request.getInstructorName())
                .instructorBio(request.getInstructorBio())
                .curriculum(request.getCurriculum())
                .targetAudience(request.getTargetAudience())
                .preparation(request.getPreparation())
                .contactInfo(request.getContactInfo())
                .hostInstagramId(request.getHostInstagramId())
                .startDateTime(request.getStartDateTime())
                .endDateTime(request.getEndDateTime())
                .applicationStartDateTime(request.getApplicationStartDateTime())
                .applicationEndDateTime(request.getApplicationEndDateTime())
                .locationName(request.getLocationName())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .region(request.getRegion())
                .maxCapacity(request.getMaxCapacity())
                .price(request.getPrice())
                .paymentGuide(request.getPaymentGuide())
                .refundPolicy(request.getRefundPolicy())
                .status(SeminarStatus.RECRUITING)
                .manualClosed(false)
                .build();

        seminar.synchronizeStatus(now(), 0);
        Seminar saved = seminarRepository.save(seminar);
        return SeminarResponse.from(saved, 0, null, false);
    }

    @Transactional
    public Page<SeminarResponse> findAll(
            Region region,
            SeminarStatus status,
            String keyword,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable,
            Long viewerUserId
    ) {
        validateSearchDateRange(from, to);
        syncExpiredSeminars();

        Pageable pageableWithSort = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("startDateTime").ascending());

        Page<Seminar> page = seminarRepository.searchVisible(
                viewerUserId,
                region,
                status,
                normalizeKeyword(keyword),
                from,
                to,
                pageableWithSort
        );

        return toSeminarResponsePage(page, viewerUserId);
    }

    @Transactional
    public SeminarResponse findById(Long seminarId, Long viewerUserId) {
        Seminar seminar = seminarRepository.findByIdAndIsHiddenFalse(seminarId)
                .orElseThrow(() -> BusinessException.notFound("세미나를 찾을 수 없습니다"));

        validateNotBlockedHost(viewerUserId, seminar.getHost().getId());

        int appliedCount = countApplied(seminar.getId());
        seminar.synchronizeStatus(now(), appliedCount);
        SeminarApplicationStatus myStatus = findMyApplicationStatus(viewerUserId, seminar.getId());
        return SeminarResponse.from(seminar, appliedCount, myStatus, hasReported(viewerUserId, seminarId));
    }

    @Transactional
    public SeminarResponse update(Long hostId, Long seminarId, SeminarUpdateRequest request) {
        Seminar seminar = seminarRepository.findByIdAndIsHiddenFalse(seminarId)
                .orElseThrow(() -> BusinessException.notFound("세미나를 찾을 수 없습니다"));

        validateHost(seminar, hostId);
        if (request.isEmpty()) {
            throw BusinessException.badRequest("수정할 필드를 1개 이상 전달해야 합니다");
        }

        int appliedCount = countApplied(seminar.getId());
        seminar.synchronizeStatus(now(), appliedCount);
        validateUpdatable(seminar);

        LocalDateTime effectiveStart = request.getStartDateTime() != null
                ? request.getStartDateTime()
                : seminar.getStartDateTime();
        LocalDateTime effectiveEnd = request.getEndDateTime() != null
                ? request.getEndDateTime()
                : seminar.getEndDateTime();
        LocalDateTime effectiveApplicationStart = request.getApplicationStartDateTime() != null
                ? request.getApplicationStartDateTime()
                : seminar.getApplicationStartDateTime();
        LocalDateTime effectiveApplicationEnd = request.getApplicationEndDateTime() != null
                ? request.getApplicationEndDateTime()
                : seminar.getApplicationEndDateTime();

        validateDateRange(effectiveStart, effectiveEnd);
        validateApplicationPeriod(effectiveApplicationStart, effectiveApplicationEnd, effectiveStart);
        validateCapacity(request.getMaxCapacity());
        validatePrice(request.getPrice());
        validateUpdateCoordinates(request);
        if (request.getMaxCapacity() != null && request.getMaxCapacity() != -1
                && request.getMaxCapacity() < appliedCount) {
            throw BusinessException.badRequest("정원을 현재 신청 인원(" + appliedCount + "명)보다 작게 줄일 수 없습니다");
        }

        boolean scheduleChanged = hasScheduleOrLocationChanged(seminar, request);
        boolean participantVisibleChange = scheduleChanged
                || isChanged(seminar.getPrice(), request.getPrice())
                || isChanged(seminar.getMaxCapacity(), request.getMaxCapacity());
        List<Long> appliedUserIds = participantVisibleChange ? findAppliedUserIds(seminar.getId()) : List.of();

        seminar.update(
                request.getTitle(),
                request.getDescription(),
                request.getMainImageUrl(),
                request.getInstructorName(),
                request.getInstructorBio(),
                request.getCurriculum(),
                request.getTargetAudience(),
                request.getPreparation(),
                request.getContactInfo(),
                request.getHostInstagramId(),
                request.getStartDateTime(),
                request.getEndDateTime(),
                request.getApplicationStartDateTime(),
                request.getApplicationEndDateTime(),
                request.getLocationName(),
                request.getAddress(),
                request.getRegion(),
                request.getMaxCapacity(),
                request.getPrice(),
                request.getPaymentGuide(),
                request.getRefundPolicy()
        );
        applyCoordinateUpdate(seminar, request);
        seminar.synchronizeStatus(now(), appliedCount);

        if (participantVisibleChange && !appliedUserIds.isEmpty()) {
            eventPublisher.publishEvent(new SeminarUpdatedEvent(seminar.getId(), seminar.getTitle(), appliedUserIds));
        }

        return SeminarResponse.from(seminar, appliedCount, null, false);
    }

    @Transactional
    public void delete(Long hostId, Long seminarId) {
        Seminar seminar = seminarRepository.findByIdAndIsHiddenFalse(seminarId)
                .orElseThrow(() -> BusinessException.notFound("세미나를 찾을 수 없습니다"));

        validateHost(seminar, hostId);
        LocalDateTime now = now();
        seminar.hide(now);
        List<SeminarApplication> appliedApplications = seminarApplicationRepository
                .findAllBySeminar_IdAndStatus(seminarId, SeminarApplicationStatus.APPLIED);
        appliedApplications.forEach(application -> application.cancelBySeminar(now));
        if (!appliedApplications.isEmpty()) {
            eventPublisher.publishEvent(new SeminarDeletedEvent(
                    seminarId,
                    seminar.getTitle(),
                    appliedApplications.stream().map(application -> application.getUser().getId()).toList()
            ));
        }
    }

    @Transactional
    public SeminarApplicationResponse apply(Long userId, Long seminarId) {
        Seminar seminar = seminarRepository.findByIdForUpdate(seminarId)
                .orElseThrow(() -> BusinessException.notFound("세미나를 찾을 수 없습니다"));
        User applicant = userRepository.findByIdAndIsWithdrawnFalse(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다"));

        LocalDateTime now = now();
        int appliedCount = countApplied(seminar.getId());
        seminar.synchronizeStatus(now, appliedCount);

        validateCanApply(seminar, userId, appliedCount, now);

        SeminarApplication application = seminarApplicationRepository
                .findBySeminarIdAndUserIdForUpdate(seminarId, userId)
                .orElse(null);
        if (application != null && application.isApplied()) {
            throw new BusinessException("ALREADY_APPLIED", "이미 신청한 세미나입니다", HttpStatus.BAD_REQUEST);
        }
        if (application == null) {
            application = SeminarApplication.builder()
                    .seminar(seminar)
                    .user(applicant)
                    .status(SeminarApplicationStatus.APPLIED)
                    .appliedAt(now)
                    .build();
            application = seminarApplicationRepository.save(application);
        } else {
            application.reactivate(now);
        }

        seminar.synchronizeStatus(now, appliedCount + 1);
        eventPublisher.publishEvent(new SeminarAppliedEvent(seminarId, seminar.getTitle(), userId));
        return SeminarApplicationResponse.from(application);
    }

    @Transactional
    public SeminarApplicationResponse cancelMyApplication(
            Long userId,
            Long seminarId,
            SeminarCancelApplicationRequest request
    ) {
        Seminar seminar = seminarRepository.findByIdForUpdate(seminarId)
                .orElseThrow(() -> BusinessException.notFound("세미나를 찾을 수 없습니다"));
        LocalDateTime now = now();
        int appliedCount = countApplied(seminar.getId());
        seminar.synchronizeStatus(now, appliedCount);

        if (!seminar.getStartDateTime().isAfter(now)) {
            throw new BusinessException("SEMINAR_FINISHED", "이미 시작된 세미나입니다", HttpStatus.BAD_REQUEST);
        }

        SeminarApplication application = seminarApplicationRepository
                .findBySeminarIdAndUserIdForUpdate(seminarId, userId)
                .orElseThrow(() -> new BusinessException("APPLICATION_NOT_FOUND", "신청 내역을 찾을 수 없습니다", HttpStatus.NOT_FOUND));

        if (!application.isApplied()) {
            throw new BusinessException("APPLICATION_ALREADY_CANCELED", "이미 취소된 신청입니다", HttpStatus.BAD_REQUEST);
        }

        String cancelReason = request == null ? null : request.getCancelReason();
        application.cancel(cancelReason, now);
        seminar.synchronizeStatus(now, Math.max(appliedCount - 1, 0));
        eventPublisher.publishEvent(new SeminarApplicationCanceledEvent(seminarId, seminar.getTitle(), userId));
        return SeminarApplicationResponse.from(application);
    }

    @Transactional
    public Page<SeminarResponse> findMyApplications(
            Long userId,
            SeminarApplicationStatus status,
            Pageable pageable
    ) {
        syncExpiredSeminars();
        Pageable pageableWithSort = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("seminar.startDateTime").ascending());

        SeminarApplicationStatus effectiveStatus = status == null ? SeminarApplicationStatus.APPLIED : status;
        Page<SeminarApplication> page = seminarApplicationRepository.findMine(userId, effectiveStatus, pageableWithSort);
        List<Long> seminarIds = page.getContent().stream()
                .map(application -> application.getSeminar().getId())
                .toList();
        Map<Long, Integer> appliedCounts = loadAppliedCounts(seminarIds);
        Set<Long> reportedSeminarIds = loadReportedSeminarIds(userId, seminarIds);

        List<SeminarResponse> content = page.getContent().stream()
                .map(application -> {
                    Seminar seminar = application.getSeminar();
                    int appliedCount = appliedCounts.getOrDefault(seminar.getId(), 0);
                    seminar.synchronizeStatus(now(), appliedCount);
                    return SeminarResponse.from(
                            seminar,
                            appliedCount,
                            application.getStatus(),
                            reportedSeminarIds.contains(seminar.getId())
                    );
                })
                .toList();

        return new PageImpl<>(content, pageableWithSort, page.getTotalElements());
    }

    @Transactional
    public Page<SeminarResponse> findMyHostedSeminars(
            Long userId,
            SeminarStatus status,
            Pageable pageable
    ) {
        syncExpiredSeminars();
        Pageable pageableWithSort = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("startDateTime").ascending());

        Page<Seminar> page = status == null
                ? seminarRepository.findByHost_IdAndIsHiddenFalse(userId, pageableWithSort)
                : seminarRepository.findByHost_IdAndIsHiddenFalseAndStatus(userId, status, pageableWithSort);
        return toSeminarResponsePage(page, userId);
    }

    @Transactional(readOnly = true)
    public Page<SeminarApplicationResponse> findApplications(
            Long hostUserId,
            Long seminarId,
            SeminarApplicationStatus status,
            Pageable pageable
    ) {
        Seminar seminar = seminarRepository.findByIdAndIsHiddenFalse(seminarId)
                .orElseThrow(() -> BusinessException.notFound("세미나를 찾을 수 없습니다"));
        validateHostManager(seminar, hostUserId);

        Pageable pageableWithSort = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("appliedAt").ascending());

        SeminarApplicationStatus effectiveStatus = status == null ? SeminarApplicationStatus.APPLIED : status;
        return seminarApplicationRepository.findBySeminarId(seminarId, effectiveStatus, pageableWithSort)
                .map(SeminarApplicationResponse::from);
    }

    @Transactional
    public SeminarApplicationResponse cancelApplicationByHost(
            Long hostUserId,
            Long seminarId,
            Long applicationId,
            SeminarHostCancelApplicationRequest request
    ) {
        Seminar seminar = seminarRepository.findByIdForUpdate(seminarId)
                .orElseThrow(() -> BusinessException.notFound("세미나를 찾을 수 없습니다"));
        validateHostManager(seminar, hostUserId);

        LocalDateTime now = now();
        int appliedCount = countApplied(seminarId);
        seminar.synchronizeStatus(now, appliedCount);

        if (seminar.getStatus() == SeminarStatus.FINISHED) {
            throw new BusinessException("SEMINAR_FINISHED", "이미 종료된 세미나입니다", HttpStatus.BAD_REQUEST);
        }

        SeminarApplication application = seminarApplicationRepository.findByIdAndSeminar_Id(applicationId, seminarId)
                .orElseThrow(() -> new BusinessException("APPLICATION_NOT_FOUND", "신청 내역을 찾을 수 없습니다", HttpStatus.NOT_FOUND));
        if (!application.isApplied()) {
            throw new BusinessException("APPLICATION_ALREADY_CANCELED", "이미 취소된 신청입니다", HttpStatus.BAD_REQUEST);
        }

        application.cancelByHost(request == null ? null : request.getCancelReason(), now);
        seminar.synchronizeStatus(now, Math.max(appliedCount - 1, 0));
        eventPublisher.publishEvent(new SeminarApplicationCanceledByHostEvent(
                seminarId,
                seminar.getTitle(),
                application.getUser().getId()
        ));
        return SeminarApplicationResponse.from(application);
    }

    @Transactional
    public SeminarResponse updateStatus(Long hostUserId, Long seminarId, SeminarStatusUpdateRequest request) {
        Seminar seminar = seminarRepository.findByIdForUpdate(seminarId)
                .orElseThrow(() -> BusinessException.notFound("세미나를 찾을 수 없습니다"));
        validateHostManager(seminar, hostUserId);

        LocalDateTime now = now();
        int appliedCount = countApplied(seminarId);
        seminar.synchronizeStatus(now, appliedCount);

        if (seminar.getStatus() == SeminarStatus.FINISHED) {
            throw new BusinessException("SEMINAR_FINISHED", "이미 종료된 세미나입니다", HttpStatus.BAD_REQUEST);
        }
        if (request.getStatus() == null
                || request.getStatus() == SeminarStatus.FINISHED
                || request.getStatus() == SeminarStatus.DELETED) {
            throw BusinessException.badRequest("모집 상태는 RECRUITING, CLOSED, CANCELED만 설정할 수 있습니다");
        }
        if (request.getStatus() == SeminarStatus.RECRUITING && seminar.isCapacityFull(appliedCount)) {
            throw new BusinessException("CAPACITY_FULL", "정원이 가득 찬 세미나는 모집중으로 변경할 수 없습니다", HttpStatus.BAD_REQUEST);
        }

        if (request.getStatus() == SeminarStatus.CANCELED) {
            List<SeminarApplication> appliedApplications = seminarApplicationRepository
                    .findAllBySeminar_IdAndStatus(seminarId, SeminarApplicationStatus.APPLIED);
            appliedApplications.forEach(application -> application.cancelBySeminar(request.getReason(), now));
            seminar.cancel();
            if (!appliedApplications.isEmpty()) {
                eventPublisher.publishEvent(new SeminarCanceledEvent(
                        seminarId,
                        seminar.getTitle(),
                        appliedApplications.stream().map(application -> application.getUser().getId()).toList()
                ));
            }
            return SeminarResponse.from(seminar, 0, null, false);
        }

        if (request.getStatus() == SeminarStatus.CLOSED) {
            seminar.closeRecruitmentManually();
        } else {
            seminar.reopenRecruitmentManually();
            seminar.synchronizeStatus(now, appliedCount);
        }
        return SeminarResponse.from(seminar, appliedCount, null, false);
    }

    @Transactional
    public void report(Long userId, Long seminarId, ReportReason reason, String customReason) {
        Seminar seminar = seminarRepository.findByIdForUpdate(seminarId)
                .orElseThrow(() -> BusinessException.notFound("세미나를 찾을 수 없습니다"));

        reportService.createReport(
                userId,
                ReportTargetType.SEMINAR,
                seminarId,
                seminar.getHost().getId(),
                reason,
                customReason
        );
        seminar.report();
    }

    @Transactional
    public int syncExpiredSeminars() {
        LocalDateTime now = now();
        List<Seminar> expiredSeminars = seminarRepository
                .findAllByIsHiddenFalseAndStatusNotAndEndDateTimeLessThanEqual(SeminarStatus.FINISHED, now);
        Map<Long, Integer> appliedCounts = loadAppliedCounts(expiredSeminars.stream().map(Seminar::getId).toList());
        expiredSeminars.forEach(seminar -> seminar.synchronizeStatus(now, appliedCounts.getOrDefault(seminar.getId(), 0)));
        if (!expiredSeminars.isEmpty()) {
            log.debug("Synchronized {} expired seminars to FINISHED", expiredSeminars.size());
        }
        return expiredSeminars.size();
    }

    private void validateCanApply(Seminar seminar, Long userId, int appliedCount, LocalDateTime now) {
        if (Objects.equals(seminar.getHost().getId(), userId)) {
            throw new BusinessException("HOST_CANNOT_APPLY", "자신이 주최한 세미나에는 신청할 수 없습니다", HttpStatus.BAD_REQUEST);
        }
        if (seminar.getStatus() == SeminarStatus.FINISHED) {
            throw new BusinessException("SEMINAR_FINISHED", "이미 종료된 세미나입니다", HttpStatus.BAD_REQUEST);
        }
        if (seminar.isCapacityFull(appliedCount)) {
            throw new BusinessException("CAPACITY_FULL", "정원이 초과되었습니다", HttpStatus.BAD_REQUEST);
        }
        if (seminar.getStatus() != SeminarStatus.RECRUITING) {
            throw new BusinessException("SEMINAR_NOT_RECRUITING", "모집 중인 세미나만 신청할 수 있습니다", HttpStatus.BAD_REQUEST);
        }
        if (seminar.isApplicationNotOpen(now)) {
            throw new BusinessException("APPLICATION_NOT_OPEN", "아직 신청이 시작되지 않은 세미나입니다", HttpStatus.BAD_REQUEST);
        }
        if (seminar.isApplicationClosed(now)) {
            throw new BusinessException("APPLICATION_CLOSED", "신청이 마감된 세미나입니다", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateHost(Seminar seminar, Long userId) {
        if (!Objects.equals(seminar.getHost().getId(), userId)) {
            throw BusinessException.forbidden("호스트만 세미나를 수정/삭제할 수 있습니다");
        }
    }

    private void validateHostManager(Seminar seminar, Long userId) {
        if (!Objects.equals(seminar.getHost().getId(), userId)) {
            throw BusinessException.forbidden("호스트만 신청자와 모집 상태를 관리할 수 있습니다");
        }
    }

    private void validateUpdatable(Seminar seminar) {
        if (seminar.getStatus() == SeminarStatus.FINISHED) {
            throw new BusinessException("SEMINAR_FINISHED", "이미 종료된 세미나입니다", HttpStatus.BAD_REQUEST);
        }
        if (seminar.getStatus() == SeminarStatus.CANCELED) {
            throw new BusinessException("SEMINAR_CANCELED", "취소된 세미나는 수정할 수 없습니다", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateDateRange(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            throw BusinessException.badRequest("시작/종료 시간이 필요합니다");
        }
        if (!endDateTime.isAfter(startDateTime)) {
            throw BusinessException.badRequest("종료 시간은 시작 시간보다 이후여야 합니다");
        }
    }

    private void validateApplicationPeriod(LocalDateTime applicationStartDateTime,
                                           LocalDateTime applicationEndDateTime,
                                           LocalDateTime seminarStartDateTime) {
        if (applicationStartDateTime != null && applicationEndDateTime != null
                && !applicationEndDateTime.isAfter(applicationStartDateTime)) {
            throw BusinessException.badRequest("신청 마감 시간은 신청 시작 시간보다 이후여야 합니다");
        }
        if (applicationEndDateTime != null && seminarStartDateTime != null
                && applicationEndDateTime.isAfter(seminarStartDateTime)) {
            throw BusinessException.badRequest("신청 마감 시간은 세미나 시작 시간보다 이후일 수 없습니다");
        }
    }

    private void validateSearchDateRange(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw BusinessException.badRequest("from은 to보다 이후일 수 없습니다");
        }
    }

    private void validateCapacity(Integer maxCapacity) {
        if (maxCapacity != null && maxCapacity != -1 && maxCapacity < 1) {
            throw BusinessException.badRequest("정원은 -1(무제한) 또는 1 이상이어야 합니다");
        }
    }

    private void validatePrice(Integer price) {
        if (price != null && price < 0) {
            throw BusinessException.badRequest("참가비는 0 이상이어야 합니다");
        }
    }

    private void validateCreateCoordinates(SeminarCreateRequest request) {
        validateCoordinatePairPresence(request.hasLatitudeField(), request.hasLongitudeField());
        validateCoordinates(request.getLatitude(), request.getLongitude());
    }

    private void validateUpdateCoordinates(SeminarUpdateRequest request) {
        validateCoordinatePairPresence(request.hasLatitudeField(), request.hasLongitudeField());
        validateCoordinates(request.getLatitude(), request.getLongitude());
    }

    private void validateCoordinatePairPresence(boolean hasLatitude, boolean hasLongitude) {
        if (hasLatitude != hasLongitude) {
            throw BusinessException.badRequest("위도와 경도는 함께 전달해야 합니다");
        }
    }

    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (latitude != null && (latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0)) {
            throw BusinessException.badRequest("위도는 -90부터 90 사이여야 합니다");
        }
        if (longitude != null && (longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0)) {
            throw BusinessException.badRequest("경도는 -180부터 180 사이여야 합니다");
        }
    }

    private void applyCoordinateUpdate(Seminar seminar, SeminarUpdateRequest request) {
        if (!request.hasLatitudeField() && !request.hasLongitudeField()) {
            return;
        }
        seminar.updateCoordinates(request.getLatitude(), request.getLongitude());
    }

    private boolean hasScheduleOrLocationChanged(Seminar seminar, SeminarUpdateRequest request) {
        return isChanged(seminar.getStartDateTime(), request.getStartDateTime())
                || isChanged(seminar.getEndDateTime(), request.getEndDateTime())
                || isChanged(seminar.getApplicationStartDateTime(), request.getApplicationStartDateTime())
                || isChanged(seminar.getApplicationEndDateTime(), request.getApplicationEndDateTime())
                || isChanged(seminar.getLocationName(), request.getLocationName())
                || isChanged(seminar.getAddress(), request.getAddress())
                || isChanged(seminar.getRegion(), request.getRegion())
                || willCoordinatesChange(seminar, request);
    }

    private boolean willCoordinatesChange(Seminar seminar, SeminarUpdateRequest request) {
        if (!request.hasLatitudeField() && !request.hasLongitudeField()) {
            return false;
        }
        return !Objects.equals(seminar.getLatitude(), request.getLatitude())
                || !Objects.equals(seminar.getLongitude(), request.getLongitude());
    }

    private boolean isChanged(Object currentValue, Object newValue) {
        return newValue != null && !Objects.equals(currentValue, newValue);
    }

    private void validateNotBlockedHost(Long viewerUserId, Long hostUserId) {
        if (viewerUserId == null) {
            return;
        }
        if (getBlockedUserIds(viewerUserId).contains(hostUserId)) {
            throw BusinessException.notFound("세미나를 찾을 수 없습니다");
        }
    }

    private List<Long> getBlockedUserIds(Long viewerUserId) {
        if (viewerUserId == null) {
            return List.of();
        }
        List<Long> blockedUserIds = userRepository.findBlockedUserIdsByUserId(viewerUserId);
        return blockedUserIds != null ? blockedUserIds : List.of();
    }

    private Page<SeminarResponse> toSeminarResponsePage(Page<Seminar> page, Long viewerUserId) {
        List<Seminar> seminars = page.getContent();
        List<Long> seminarIds = seminars.stream().map(Seminar::getId).toList();
        Map<Long, Integer> appliedCounts = loadAppliedCounts(seminarIds);
        Map<Long, SeminarApplicationStatus> myStatuses = loadMyApplicationStatuses(
                viewerUserId,
                seminarIds
        );
        Set<Long> reportedSeminarIds = loadReportedSeminarIds(viewerUserId, seminarIds);

        LocalDateTime now = now();
        return page.map(seminar -> {
            int appliedCount = appliedCounts.getOrDefault(seminar.getId(), 0);
            seminar.synchronizeStatus(now, appliedCount);
            return SeminarResponse.from(
                    seminar,
                    appliedCount,
                    myStatuses.get(seminar.getId()),
                    reportedSeminarIds.contains(seminar.getId())
            );
        });
    }

    private Map<Long, Integer> loadAppliedCounts(Collection<Long> seminarIds) {
        if (seminarIds == null || seminarIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return seminarApplicationRepository
                .countBySeminarIdsAndStatus(seminarIds, SeminarApplicationStatus.APPLIED)
                .stream()
                .collect(Collectors.toMap(
                        SeminarApplicationRepository.SeminarApplicationCountView::getSeminarId,
                        view -> Math.toIntExact(view.getApplicationCount()),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private Map<Long, SeminarApplicationStatus> loadMyApplicationStatuses(Long viewerUserId, Collection<Long> seminarIds) {
        if (viewerUserId == null || seminarIds == null || seminarIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return seminarApplicationRepository.findAllByUserIdAndSeminarIdIn(viewerUserId, seminarIds)
                .stream()
                .collect(Collectors.toMap(
                        application -> application.getSeminar().getId(),
                        SeminarApplication::getStatus,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private List<Long> findAppliedUserIds(Long seminarId) {
        return seminarApplicationRepository.findAllBySeminar_IdAndStatus(seminarId, SeminarApplicationStatus.APPLIED)
                .stream()
                .map(application -> application.getUser().getId())
                .toList();
    }

    private boolean hasReported(Long viewerUserId, Long seminarId) {
        if (viewerUserId == null) {
            return false;
        }
        return reportRepository.existsByReporter_IdAndTargetTypeAndTargetId(
                viewerUserId,
                ReportTargetType.SEMINAR,
                seminarId
        );
    }

    private Set<Long> loadReportedSeminarIds(Long viewerUserId, Collection<Long> seminarIds) {
        if (viewerUserId == null || seminarIds == null || seminarIds.isEmpty()) {
            return Collections.emptySet();
        }
        return reportRepository.findTargetIdsByReporter_IdAndTargetTypeAndTargetIdIn(
                        viewerUserId,
                        ReportTargetType.SEMINAR,
                        seminarIds
                ).stream()
                .collect(Collectors.toSet());
    }

    private SeminarApplicationStatus findMyApplicationStatus(Long viewerUserId, Long seminarId) {
        if (viewerUserId == null) {
            return null;
        }
        return seminarApplicationRepository.findBySeminar_IdAndUser_Id(seminarId, viewerUserId)
                .map(SeminarApplication::getStatus)
                .orElse(null);
    }

    private int countApplied(Long seminarId) {
        return Math.toIntExact(seminarApplicationRepository.countBySeminar_IdAndStatus(
                seminarId,
                SeminarApplicationStatus.APPLIED
        ));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }
}
