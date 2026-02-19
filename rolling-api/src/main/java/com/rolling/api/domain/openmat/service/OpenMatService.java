package com.rolling.api.domain.openmat.service;

import com.rolling.api.domain.openmat.dto.OpenMatCreateRequest;
import com.rolling.api.domain.openmat.dto.OpenMatResponse;
import com.rolling.api.domain.openmat.entity.OpenMat;
import com.rolling.api.domain.openmat.entity.OpenMatStatus;
import com.rolling.api.domain.openmat.repository.OpenMatRepository;
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

import com.rolling.api.domain.openmat.dto.OpenMatUpdateRequest;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OpenMatService {

    private final OpenMatRepository openMatRepository;
    private final UserRepository userRepository;

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
                .maxCapacity(request.getMaxCapacity())
                .hostInstagramId(request.getHostInstagramId())
                .status(OpenMatStatus.RECRUITING)
                .build();

        OpenMat saved = openMatRepository.save(openMat);
        return OpenMatResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<OpenMatResponse> findAll(String region, Pageable pageable) {
        Pageable pageableWithSort = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("startDateTime").ascending());

        Page<OpenMat> page;
        if (region != null && !region.isBlank()) {
            page = openMatRepository.findByIsHiddenFalseAndAddressContaining(region, pageableWithSort);
        } else {
            page = openMatRepository.findByIsHiddenFalse(pageableWithSort);
        }

        return page.map(OpenMatResponse::from);
    }

    @Transactional(readOnly = true)
    public OpenMatResponse findById(Long id) {
        OpenMat openMat = openMatRepository.findByIdAndIsHiddenFalse(id)
                .orElseThrow(() -> BusinessException.notFound("오픈매트를 찾을 수 없습니다"));
        return OpenMatResponse.from(openMat);
    }

    @Transactional
    public OpenMatResponse update(Long userId, Long openMatId, OpenMatUpdateRequest request) {
        OpenMat openMat = openMatRepository.findByIdAndIsHiddenFalse(openMatId)
                .orElseThrow(() -> BusinessException.notFound("오픈매트를 찾을 수 없습니다"));

        validateHost(openMat, userId);

        openMat.update(
                request.getTitle(),
                request.getDescription(),
                request.getStartDateTime(),
                request.getEndDateTime(),
                request.getLocationName(),
                request.getAddress(),
                request.getMaxCapacity(),
                request.getHostInstagramId()
        );

        return OpenMatResponse.from(openMat);
    }

    @Transactional
    public void delete(Long userId, Long openMatId, boolean force) {
        OpenMat openMat = openMatRepository.findByIdAndIsHiddenFalse(openMatId)
                .orElseThrow(() -> BusinessException.notFound("오픈매트를 찾을 수 없습니다"));

        validateHost(openMat, userId);

        if (openMat.hasParticipants() && !force) {
            throw BusinessException.badRequest("신청자가 있는 오픈매트는 force=true 파라미터가 필요합니다");
        }

        openMatRepository.delete(openMat);
    }

    @Transactional
    public void apply(Long userId, Long openMatId) {
        OpenMat openMat = openMatRepository.findByIdAndIsHiddenFalse(openMatId)
                .orElseThrow(() -> BusinessException.notFound("오픈매트를 찾을 수 없습니다"));

        if (openMat.getStatus() == OpenMatStatus.CLOSED) {
            throw new BusinessException("OPEN_MAT_CLOSED", "모집이 마감된 오픈매트입니다", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        if (openMat.getStatus() == OpenMatStatus.FINISHED) {
            throw new BusinessException("OPEN_MAT_FINISHED", "이미 종료된 오픈매트입니다", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        if (openMat.isParticipant(userId)) {
            throw new BusinessException("ALREADY_APPLIED", "이미 신청한 오픈매트입니다", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        if (openMat.isCapacityFull()) {
            throw new BusinessException("CAPACITY_FULL", "정원이 초과되었습니다", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        openMat.addParticipant(userId);
    }

    @Transactional
    public void cancelApply(Long userId, Long openMatId) {
        OpenMat openMat = openMatRepository.findByIdAndIsHiddenFalse(openMatId)
                .orElseThrow(() -> BusinessException.notFound("오픈매트를 찾을 수 없습니다"));

        if (!openMat.isParticipant(userId)) {
            throw BusinessException.badRequest("신청하지 않은 오픈매트입니다");
        }

        openMat.removeParticipant(userId);
    }

    @Transactional(readOnly = true)
    public Page<OpenMatResponse> findMyOpenMats(Long userId, Pageable pageable) {
        Pageable pageableWithSort = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("startDateTime").ascending());
        return openMatRepository.findByParticipantUidsContaining(userId, pageableWithSort)
                .map(OpenMatResponse::from);
    }

    private void validateHost(OpenMat openMat, Long userId) {
        if (!openMat.getHost().getId().equals(userId)) {
            throw BusinessException.forbidden("작성자만 수정/삭제할 수 있습니다");
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
        if (maxCapacity != null && maxCapacity < -1) {
            throw BusinessException.badRequest("정원은 -1 이상이어야 합니다");
        }
    }
}
