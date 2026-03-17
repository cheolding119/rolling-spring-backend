package com.rolling.api.domain.openmat.service;

import com.rolling.api.domain.openmat.config.OpenMatTestingAccessConfig;
import com.rolling.api.domain.openmat.dto.OpenMatCreateRequest;
import com.rolling.api.domain.openmat.dto.OpenMatResponse;
import com.rolling.api.domain.openmat.dto.OpenMatUpdateRequest;
import com.rolling.api.domain.openmat.event.OpenMatDeletedEvent;
import com.rolling.api.domain.openmat.event.OpenMatUpdatedEvent;
import com.rolling.api.domain.openmat.entity.OpenMat;
import com.rolling.api.domain.openmat.entity.OpenMatStatus;
import com.rolling.api.domain.openmat.entity.Region;
import com.rolling.api.domain.openmat.repository.OpenMatRepository;
import com.rolling.api.domain.report.entity.ReportReason;
import com.rolling.api.domain.report.entity.ReportTargetType;
import com.rolling.api.domain.report.service.ReportService;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenMatService {

    private final OpenMatRepository openMatRepository;
    private final UserRepository userRepository;
    private final ReportService reportService;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;
    private final OpenMatTestingAccessConfig openMatTestingAccessConfig;

    @Transactional
    public OpenMatResponse create(Long hostId, OpenMatCreateRequest request) {
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> BusinessException.notFound("호스트 사용자를 찾을 수 없습니다"));

        validateDateRange(request.getStartDateTime(), request.getEndDateTime());
        validateCapacity(request.getMaxCapacity());

        OpenMat openMat = OpenMat.builder()
                .host(host)
                .title(request.getTitle())
                .description(request.getDescription())
                .startDateTime(request.getStartDateTime())
                .endDateTime(request.getEndDateTime())
                .locationName(request.getLocationName())
                .address(request.getAddress())
                .region(request.getRegion())
                .maxCapacity(request.getMaxCapacity())
                .hostInstagramId(request.getHostInstagramId())
                .status(OpenMatStatus.RECRUITING)
                .build();

        openMat.synchronizeStatus(now());
        OpenMat saved = openMatRepository.save(openMat);
        return OpenMatResponse.from(saved);
    }

    @Transactional
    public Page<OpenMatResponse> findAll(Region region, OpenMatStatus status, Pageable pageable) {
        return findAll(region, status, null, pageable);
    }

    @Transactional
    public Page<OpenMatResponse> findAll(Region region, OpenMatStatus status, String keyword, Pageable pageable) {
        syncExpiredOpenMats();

        Pageable pageableWithSort = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("startDateTime").ascending());

        Page<OpenMat> page = findOpenMats(region, status, normalizeKeyword(keyword), pageableWithSort);
        LocalDateTime now = now();
        page.getContent().forEach(openMat -> openMat.synchronizeStatus(now));

        return page.map(OpenMatResponse::from);
    }

    @Transactional
    public OpenMatResponse findById(Long id) {
        OpenMat openMat = openMatRepository.findByIdAndIsHiddenFalse(id)
                .orElseThrow(() -> BusinessException.notFound("오픈매트를 찾을 수 없습니다"));
        openMat.synchronizeStatus(now());
        return OpenMatResponse.from(openMat);
    }

    @Transactional
    public OpenMatResponse update(Long userId, Long openMatId, OpenMatUpdateRequest request) {
        OpenMat openMat = openMatRepository.findByIdAndIsHiddenFalse(openMatId)
                .orElseThrow(() -> BusinessException.notFound("오픈매트를 찾을 수 없습니다"));

        if (shouldBypassUnauthenticatedUpdate(userId)) {
            log.warn("Allowing unauthenticated open mat update for testing - openMatId: {}", openMatId);
        } else {
            validateHost(openMat, userId);
        }
        boolean startDateTimeChanged = isChanged(openMat.getStartDateTime(), request.getStartDateTime());
        boolean endDateTimeChanged = isChanged(openMat.getEndDateTime(), request.getEndDateTime());
        boolean locationNameChanged = isChanged(openMat.getLocationName(), request.getLocationName());
        boolean addressChanged = isChanged(openMat.getAddress(), request.getAddress());
        boolean regionChanged = isChanged(openMat.getRegion(), request.getRegion());
        boolean hasParticipants = openMat.hasParticipants();
        boolean shouldNotifyParticipants = hasParticipants
                && (startDateTimeChanged || endDateTimeChanged || locationNameChanged || addressChanged || regionChanged);
        List<Long> participantUserIds = List.copyOf(openMat.getParticipantUids());

        LocalDateTime effectiveStart = request.getStartDateTime() != null ? request.getStartDateTime() : openMat.getStartDateTime();
        LocalDateTime effectiveEnd = request.getEndDateTime() != null ? request.getEndDateTime() : openMat.getEndDateTime();
        validateDateRange(effectiveStart, effectiveEnd);

        validateCapacity(request.getMaxCapacity());

        if (request.getMaxCapacity() != null && request.getMaxCapacity() != -1
                && request.getMaxCapacity() < openMat.getParticipantUids().size()) {
            throw BusinessException.badRequest(
                    "정원을 현재 참여자 수(" + openMat.getParticipantUids().size() + "명)보다 작게 줄일 수 없습니다");
        }

        openMat.update(
                request.getTitle(),
                request.getDescription(),
                request.getStartDateTime(),
                request.getEndDateTime(),
                request.getLocationName(),
                request.getAddress(),
                request.getRegion(),
                request.getMaxCapacity(),
                request.getHostInstagramId()
        );
        openMat.synchronizeStatus(now());

        log.info(
                "OpenMat update notification check. openMatId={}, participantUserIds={}, hasParticipants={}, startDateTimeChanged={}, endDateTimeChanged={}, locationNameChanged={}, addressChanged={}, regionChanged={}, shouldNotifyParticipants={}",
                openMat.getId(),
                participantUserIds,
                hasParticipants,
                startDateTimeChanged,
                endDateTimeChanged,
                locationNameChanged,
                addressChanged,
                regionChanged,
                shouldNotifyParticipants
        );

        if (shouldNotifyParticipants) {
            eventPublisher.publishEvent(new OpenMatUpdatedEvent(openMat.getId(), openMat.getTitle(), participantUserIds));
        }

        return OpenMatResponse.from(openMat);
    }

    @Transactional
    public void delete(Long userId, Long openMatId, boolean force) {
        OpenMat openMat = openMatRepository.findByIdAndIsHiddenFalse(openMatId)
                .orElseThrow(() -> BusinessException.notFound("오픈매트를 찾을 수 없습니다"));

        if (shouldBypassUnauthenticatedDelete(userId)) {
            log.warn("Allowing unauthenticated open mat delete for testing - openMatId: {}", openMatId);
        } else {
            validateHost(openMat, userId);
        }
        List<Long> participantUserIds = List.copyOf(openMat.getParticipantUids());

        if (openMat.hasParticipants() && !force) {
            throw BusinessException.badRequest("신청자가 있는 오픈매트는 force=true 파라미터가 필요합니다");
        }

        openMat.hide();

        if (!participantUserIds.isEmpty()) {
            eventPublisher.publishEvent(new OpenMatDeletedEvent(openMat.getId(), openMat.getTitle(), participantUserIds));
        }
    }

    @Transactional
    public void apply(Long userId, Long openMatId) {
        OpenMat openMat = openMatRepository.findByIdForUpdate(openMatId)
                .orElseThrow(() -> BusinessException.notFound("오픈매트를 찾을 수 없습니다"));
        LocalDateTime now = now();
        openMat.synchronizeStatus(now);

        if (openMat.getHost().getId().equals(userId)) {
            throw new BusinessException("HOST_CANNOT_APPLY", "자신이 주최한 오픈매트에는 신청할 수 없습니다", HttpStatus.BAD_REQUEST);
        }
        if (openMat.isReported()) {
            throw new BusinessException("OPEN_MAT_REPORTED", "신고 누적으로 신청이 차단된 오픈매트입니다", HttpStatus.BAD_REQUEST);
        }
        if (openMat.getStatus() == OpenMatStatus.CLOSED) {
            throw new BusinessException("OPEN_MAT_CLOSED", "모집이 마감된 오픈매트입니다", HttpStatus.BAD_REQUEST);
        }
        if (openMat.getStatus() == OpenMatStatus.FINISHED) {
            throw new BusinessException("OPEN_MAT_FINISHED", "이미 종료된 오픈매트입니다", HttpStatus.BAD_REQUEST);
        }
        if (openMat.isParticipant(userId)) {
            throw new BusinessException("ALREADY_APPLIED", "이미 신청한 오픈매트입니다", HttpStatus.BAD_REQUEST);
        }
        if (openMat.isCapacityFull()) {
            throw new BusinessException("CAPACITY_FULL", "정원이 초과되었습니다", HttpStatus.BAD_REQUEST);
        }

        openMat.addParticipant(userId);
        openMat.synchronizeStatus(now);
    }

    @Transactional
    public void cancelApply(Long userId, Long openMatId) {
        OpenMat openMat = openMatRepository.findByIdForUpdate(openMatId)
                .orElseThrow(() -> BusinessException.notFound("오픈매트를 찾을 수 없습니다"));
        LocalDateTime now = now();
        openMat.synchronizeStatus(now);

        if (!openMat.isParticipant(userId)) {
            throw BusinessException.badRequest("신청하지 않은 오픈매트입니다");
        }

        openMat.removeParticipant(userId);
        openMat.synchronizeStatus(now);
    }

    @Transactional
    public void report(Long userId, Long openMatId, ReportReason reason, String customReason) {
        OpenMat openMat = openMatRepository.findByIdForUpdate(openMatId)
                .orElseThrow(() -> BusinessException.notFound("오픈매트를 찾을 수 없습니다"));

        reportService.createReport(
                userId,
                ReportTargetType.OPEN_MAT,
                openMatId,
                openMat.getHost().getId(),
                reason,
                customReason
        );
        openMat.report();
    }

    @Transactional
    public Page<OpenMatResponse> findMyOpenMats(Long userId, Pageable pageable) {
        syncExpiredOpenMats();

        Pageable pageableWithSort = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("startDateTime").ascending());

        Page<OpenMat> page = openMatRepository.findByParticipantUidsContaining(userId, pageableWithSort);
        LocalDateTime now = now();
        page.getContent().forEach(openMat -> openMat.synchronizeStatus(now));
        return page.map(OpenMatResponse::from);
    }

    @Transactional
    public int syncExpiredOpenMats() {
        LocalDateTime now = now();
        List<OpenMat> expiredOpenMats = openMatRepository
                .findAllByIsHiddenFalseAndStatusNotAndEndDateTimeLessThanEqual(OpenMatStatus.FINISHED, now);

        expiredOpenMats.forEach(openMat -> openMat.synchronizeStatus(now));

        if (!expiredOpenMats.isEmpty()) {
            log.debug("Synchronized {} expired open mats to FINISHED", expiredOpenMats.size());
        }
        return expiredOpenMats.size();
    }

    private void validateHost(OpenMat openMat, Long userId) {
        if (!openMat.getHost().getId().equals(userId)) {
            throw BusinessException.forbidden("작성자만 수정/삭제할 수 있습니다");
        }
    }

    private boolean shouldBypassUnauthenticatedUpdate(Long userId) {
        return userId == null && openMatTestingAccessConfig.isAllowUnauthenticatedUpdate();
    }

    private boolean shouldBypassUnauthenticatedDelete(Long userId) {
        return userId == null && openMatTestingAccessConfig.isAllowUnauthenticatedUpdate();
    }

    private void validateDateRange(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            throw BusinessException.badRequest("시작/종료 시간이 필요합니다");
        }
        if (!endDateTime.isAfter(startDateTime)) {
            throw BusinessException.badRequest("종료 시간은 시작 시간보다 이후여야 합니다");
        }
    }

    private void validateCapacity(Integer maxCapacity) {
        if (maxCapacity != null && maxCapacity != -1 && maxCapacity < 1) {
            throw BusinessException.badRequest("정원은 -1(무제한) 또는 1 이상이어야 합니다");
        }
    }

    private boolean hasScheduleOrLocationChanged(OpenMat openMat, OpenMatUpdateRequest request) {
        return isChanged(openMat.getStartDateTime(), request.getStartDateTime())
                || isChanged(openMat.getEndDateTime(), request.getEndDateTime())
                || isChanged(openMat.getLocationName(), request.getLocationName())
                || isChanged(openMat.getAddress(), request.getAddress())
                || isChanged(openMat.getRegion(), request.getRegion());
    }

    private boolean isChanged(Object currentValue, Object newValue) {
        return newValue != null && !Objects.equals(currentValue, newValue);
    }

    private Page<OpenMat> findOpenMats(Region region, OpenMatStatus status, String keyword, Pageable pageable) {
        if (keyword != null) {
            return openMatRepository.searchVisible(region, status, keyword, pageable);
        }
        if (region != null && status != null) {
            return openMatRepository.findByIsHiddenFalseAndRegionAndStatus(region, status, pageable);
        }
        if (region != null) {
            return openMatRepository.findByIsHiddenFalseAndRegion(region, pageable);
        }
        if (status != null) {
            return openMatRepository.findByIsHiddenFalseAndStatus(status, pageable);
        }
        return openMatRepository.findByIsHiddenFalse(pageable);
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

