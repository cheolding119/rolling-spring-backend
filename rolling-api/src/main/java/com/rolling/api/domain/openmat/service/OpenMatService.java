package com.rolling.api.domain.openmat.service;

import com.rolling.api.domain.notification.model.PushNotificationType;
import com.rolling.api.domain.notification.repository.NotificationRepository;
import com.rolling.api.domain.openmat.dto.OpenMatCreateRequest;
import com.rolling.api.domain.openmat.dto.OpenMatHostStatusUpdateRequest;
import com.rolling.api.domain.openmat.dto.OpenMatParticipantResponse;
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
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenMatService {

    private final OpenMatRepository openMatRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ReportService reportService;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;
    private MeterRegistry meterRegistry;

    @Autowired(required = false)
    void setMeterRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

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
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("startDateTime").descending());

        Page<OpenMat> page = findOpenMats(region, status, normalizeKeyword(keyword), pageableWithSort);
        return toOpenMatResponsePage(page);
    }

    @Transactional
    public OpenMatResponse findById(Long id) {
        return findById(id, null, false);
    }

    @Transactional
    public OpenMatResponse findById(Long id, Long requesterUserId, boolean requesterIsAdmin) {
        OpenMat openMat = openMatRepository.findByIdAndIsHiddenFalse(id)
                .orElseGet(() -> openMatRepository.findById(id)
                        .orElseThrow(() -> BusinessException.notFound("오픈매트를 찾을 수 없습니다")));

        if (Boolean.TRUE.equals(openMat.getIsHidden()) && !canViewDeletedOpenMat(openMat, requesterUserId, requesterIsAdmin)) {
            throw BusinessException.notFound("오픈매트를 찾을 수 없습니다");
        }

        if (!Boolean.TRUE.equals(openMat.getIsHidden())) {
            openMat.synchronizeStatus(now());
        }
        return OpenMatResponse.from(openMat);
    }

    @Transactional
    public OpenMatResponse update(Long userId, Long openMatId, OpenMatUpdateRequest request) {
        OpenMat openMat = openMatRepository.findByIdAndIsHiddenFalse(openMatId)
                .orElseThrow(() -> BusinessException.notFound("오픈매트를 찾을 수 없습니다"));

        validateHost(openMat, userId);
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
    public void delete(Long userId, Long openMatId) {
        OpenMat openMat = openMatRepository.findByIdAndIsHiddenFalse(openMatId)
                .orElseThrow(() -> BusinessException.notFound("오픈매트를 찾을 수 없습니다"));

        validateHost(openMat, userId);
        List<Long> participantUserIds = List.copyOf(openMat.getParticipantUids());

        openMat.hide(now());

        if (!participantUserIds.isEmpty()) {
            eventPublisher.publishEvent(new OpenMatDeletedEvent(openMat.getId(), openMat.getTitle(), participantUserIds));
        }
    }

    @Transactional
    public void apply(Long userId, Long openMatId) {
        OpenMat openMat = openMatRepository.findByIdForUpdate(openMatId)
                .orElseThrow(() -> {
                    incrementOpenMatCounter("rolling_openmat_apply_total", "not_found", 1);
                    return BusinessException.notFound("오픈매트를 찾을 수 없습니다");
                });
        LocalDateTime now = now();
        openMat.synchronizeStatus(now);

        if (openMat.getHost().getId().equals(userId)) {
            incrementOpenMatCounter("rolling_openmat_apply_total", "host_cannot_apply", 1);
            throw new BusinessException("HOST_CANNOT_APPLY", "자신이 주최한 오픈매트에는 신청할 수 없습니다", HttpStatus.BAD_REQUEST);
        }
        if (openMat.isReported()) {
            incrementOpenMatCounter("rolling_openmat_apply_total", "open_mat_reported", 1);
            throw new BusinessException("OPEN_MAT_REPORTED", "신고 누적으로 신청이 차단된 오픈매트입니다", HttpStatus.BAD_REQUEST);
        }
        if (openMat.getStatus() == OpenMatStatus.CLOSED) {
            incrementOpenMatCounter("rolling_openmat_apply_total", "open_mat_closed", 1);
            throw new BusinessException("OPEN_MAT_CLOSED", "모집이 마감된 오픈매트입니다", HttpStatus.BAD_REQUEST);
        }
        if (openMat.getStatus() == OpenMatStatus.FINISHED) {
            incrementOpenMatCounter("rolling_openmat_apply_total", "open_mat_finished", 1);
            throw new BusinessException("OPEN_MAT_FINISHED", "이미 종료된 오픈매트입니다", HttpStatus.BAD_REQUEST);
        }
        if (openMat.isParticipant(userId)) {
            incrementOpenMatCounter("rolling_openmat_apply_total", "already_applied", 1);
            throw new BusinessException("ALREADY_APPLIED", "이미 신청한 오픈매트입니다", HttpStatus.BAD_REQUEST);
        }
        if (openMat.isCapacityFull()) {
            incrementOpenMatCounter("rolling_openmat_apply_total", "capacity_full", 1);
            throw new BusinessException("CAPACITY_FULL", "정원이 초과되었습니다", HttpStatus.BAD_REQUEST);
        }

        openMat.addParticipant(userId);
        openMat.synchronizeStatus(now);
        incrementOpenMatCounter("rolling_openmat_apply_total", "success", 1);
    }

    @Transactional
    public void cancelApply(Long userId, Long openMatId) {
        OpenMat openMat = openMatRepository.findByIdForUpdate(openMatId)
                .orElseThrow(() -> {
                    incrementOpenMatCounter("rolling_openmat_cancel_total", "not_found", 1);
                    return BusinessException.notFound("오픈매트를 찾을 수 없습니다");
                });
        LocalDateTime now = now();
        openMat.synchronizeStatus(now);

        if (!openMat.isParticipant(userId)) {
            incrementOpenMatCounter("rolling_openmat_cancel_total", "not_applied", 1);
            throw BusinessException.badRequest("신청하지 않은 오픈매트입니다");
        }

        openMat.removeParticipant(userId);
        openMat.synchronizeStatus(now);
        incrementOpenMatCounter("rolling_openmat_cancel_total", "success", 1);
    }

    @Transactional(readOnly = true)
    public List<OpenMatParticipantResponse> findParticipants(Long hostUserId, Long openMatId) {
        OpenMat openMat = openMatRepository.findByIdAndIsHiddenFalse(openMatId)
                .orElseThrow(() -> BusinessException.notFound("오픈매트를 찾을 수 없습니다"));

        validateHostManager(openMat, hostUserId);

        if (openMat.getParticipantUids().isEmpty()) {
            return List.of();
        }

        List<User> participants = userRepository.findAllByIdInAndIsWithdrawnFalse(openMat.getParticipantUids());
        Map<Long, User> participantMap = new LinkedHashMap<>();
        participants.forEach(user -> participantMap.put(user.getId(), user));

        return openMat.getParticipantUids().stream()
                .map(participantMap::get)
                .filter(Objects::nonNull)
                .map(OpenMatParticipantResponse::from)
                .toList();
    }

    @Transactional
    public void removeParticipant(Long hostUserId, Long openMatId, Long participantUserId) {
        OpenMat openMat = openMatRepository.findByIdForUpdate(openMatId)
                .orElseThrow(() -> BusinessException.notFound("오픈매트를 찾을 수 없습니다"));
        validateHostManager(openMat, hostUserId);

        LocalDateTime now = now();
        openMat.synchronizeStatus(now);

        if (openMat.getStatus() == OpenMatStatus.FINISHED) {
            throw new BusinessException("OPEN_MAT_FINISHED", "이미 종료된 오픈매트입니다", HttpStatus.BAD_REQUEST);
        }
        if (!openMat.isParticipant(participantUserId)) {
            throw new BusinessException("PARTICIPANT_NOT_FOUND", "해당 참가자를 찾을 수 없습니다", HttpStatus.NOT_FOUND);
        }

        openMat.removeParticipant(participantUserId);
        openMat.synchronizeStatus(now);
    }

    @Transactional
    public OpenMatResponse updateHostingStatus(Long hostUserId, Long openMatId, OpenMatHostStatusUpdateRequest request) {
        OpenMat openMat = openMatRepository.findByIdForUpdate(openMatId)
                .orElseThrow(() -> BusinessException.notFound("오픈매트를 찾을 수 없습니다"));
        validateHostManager(openMat, hostUserId);

        LocalDateTime now = now();
        openMat.synchronizeStatus(now);

        if (openMat.getStatus() == OpenMatStatus.FINISHED) {
            throw new BusinessException("OPEN_MAT_FINISHED", "이미 종료된 오픈매트입니다", HttpStatus.BAD_REQUEST);
        }
        if (request.getStatus() == null || request.getStatus() == OpenMatStatus.FINISHED) {
            throw BusinessException.badRequest("모집 상태는 RECRUITING 또는 CLOSED만 설정할 수 있습니다");
        }

        if (request.getStatus() == OpenMatStatus.CLOSED) {
            openMat.closeRecruitmentManually();
        } else {
            if (openMat.isCapacityFull()) {
                throw new BusinessException("CAPACITY_FULL", "정원이 가득 찬 오픈매트는 모집중으로 변경할 수 없습니다", HttpStatus.BAD_REQUEST);
            }
            openMat.reopenRecruitmentManually();
            openMat.synchronizeStatus(now);
        }

        return OpenMatResponse.from(openMat);
    }

    @Transactional
    public void report(Long userId, Long openMatId, ReportReason reason, String customReason) {
        OpenMat openMat = openMatRepository.findByIdForUpdate(openMatId)
                .orElseThrow(() -> {
                    incrementOpenMatCounter("rolling_openmat_report_total", "not_found", 1);
                    return BusinessException.notFound("오픈매트를 찾을 수 없습니다");
                });

        reportService.createReport(
                userId,
                ReportTargetType.OPEN_MAT,
                openMatId,
                openMat.getHost().getId(),
                reason,
                customReason
        );
        openMat.report();
        incrementOpenMatCounter("rolling_openmat_report_total", "success", 1);
    }

    @Transactional
    public OpenMatResponse clearReportBlock(Long adminUserId, Long openMatId) {
        OpenMat openMat = openMatRepository.findById(openMatId)
                .orElseThrow(() -> BusinessException.notFound("오픈매트를 찾을 수 없습니다"));

        int previousReportCount = openMat.getReportCount() == null ? 0 : openMat.getReportCount();
        openMat.clearReportBlock();
        log.info("OpenMat report block cleared. openMatId={}, adminUserId={}, previousReportCount={}",
                openMatId,
                adminUserId,
                previousReportCount);

        return OpenMatResponse.from(openMat);
    }

    @Transactional
    public Page<OpenMatResponse> findMyOpenMats(Long userId, Pageable pageable) {
        syncExpiredOpenMats();

        Pageable pageableWithSort = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("startDateTime").ascending());

        Page<OpenMat> page = openMatRepository.findByParticipantUidsContaining(userId, pageableWithSort);
        return toOpenMatResponsePage(page);
    }

    @Transactional
    public Page<OpenMatResponse> findMyHostedOpenMats(Long userId, Pageable pageable) {
        syncExpiredOpenMats();

        Pageable pageableWithSort = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("startDateTime").ascending());

        Page<OpenMat> page = openMatRepository.findByHost_IdAndIsHiddenFalse(userId, pageableWithSort);
        return toOpenMatResponsePage(page);
    }

    @Transactional
    public int syncExpiredOpenMats() {
        LocalDateTime now = now();
        List<OpenMat> expiredOpenMats = openMatRepository
                .findAllByIsHiddenFalseAndStatusNotAndEndDateTimeLessThanEqual(OpenMatStatus.FINISHED, now);
        Map<Long, Integer> participantCounts = loadParticipantCounts(expiredOpenMats);
        expiredOpenMats.forEach(openMat -> openMat.synchronizeStatus(now, resolveParticipantCount(openMat, participantCounts)));

        if (!expiredOpenMats.isEmpty()) {
            log.debug("Synchronized {} expired open mats to FINISHED", expiredOpenMats.size());
        }
        incrementOpenMatCounter("rolling_openmat_sync_total", "success", expiredOpenMats.size());
        return expiredOpenMats.size();
    }

    private boolean canViewDeletedOpenMat(OpenMat openMat, Long requesterUserId, boolean requesterIsAdmin) {
        if (requesterIsAdmin) {
            return true;
        }
        if (requesterUserId == null) {
            return false;
        }
        if (Objects.equals(openMat.getHost().getId(), requesterUserId)) {
            return true;
        }
        if (openMat.isParticipant(requesterUserId)) {
            return true;
        }
        return notificationRepository.existsByUser_IdAndTypeAndTargetId(
                requesterUserId,
                PushNotificationType.OPEN_MAT_DELETED,
                openMat.getId()
        );
    }

    private void validateHost(OpenMat openMat, Long userId) {
        if (!openMat.getHost().getId().equals(userId)) {
            throw BusinessException.forbidden("작성자만 수정/삭제할 수 있습니다");
        }
    }

    private void validateHostManager(OpenMat openMat, Long userId) {
        if (!openMat.getHost().getId().equals(userId)) {
            throw BusinessException.forbidden("작성자만 참가자/모집 상태를 관리할 수 있습니다");
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

    private Page<OpenMatResponse> toOpenMatResponsePage(Page<OpenMat> page) {
        Map<Long, Integer> participantCounts = loadParticipantCounts(page.getContent());
        LocalDateTime now = now();
        page.getContent().forEach(openMat -> openMat.synchronizeStatus(now, resolveParticipantCount(openMat, participantCounts)));
        return page.map(openMat -> OpenMatResponse.from(openMat, resolveParticipantCount(openMat, participantCounts)));
    }

    private Map<Long, Integer> loadParticipantCounts(List<OpenMat> openMats) {
        if (openMats == null || openMats.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> openMatIds = openMats.stream()
                .map(OpenMat::getId)
                .toList();
        List<OpenMatRepository.OpenMatParticipantCountView> counts = openMatRepository.countParticipantsByOpenMatIds(openMatIds);
        if (counts == null || counts.isEmpty()) {
            return Collections.emptyMap();
        }

        return counts.stream().collect(Collectors.toMap(
                OpenMatRepository.OpenMatParticipantCountView::getOpenMatId,
                count -> Math.toIntExact(count.getParticipantCount()),
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    private int resolveParticipantCount(OpenMat openMat, Map<Long, Integer> participantCounts) {
        Integer participantCount = participantCounts.get(openMat.getId());
        return participantCount != null ? participantCount : openMat.getParticipantUids().size();
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

    private void incrementOpenMatCounter(String metricName, String result, int amount) {
        if (meterRegistry == null || amount <= 0) {
            return;
        }
        meterRegistry.counter(metricName, "result", result)
                .increment(amount);
    }
}





