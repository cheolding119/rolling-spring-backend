package com.rolling.api.domain.openmat.service;

import com.rolling.api.domain.openmat.dto.OpenMatCreateRequest;
import com.rolling.api.domain.openmat.dto.OpenMatResponse;
import com.rolling.api.domain.openmat.entity.OpenMat;
import com.rolling.api.domain.openmat.entity.OpenMatStatus;
import com.rolling.api.domain.openmat.repository.OpenMatRepository;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class OpenMatService {

    private final OpenMatRepository openMatRepository;
    private final UserRepository userRepository;

    @Transactional
    public OpenMatResponse create(Long hostId, OpenMatCreateRequest request) {
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "호스트 사용자를 찾을 수 없습니다"));

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
    public Page<OpenMatResponse> findAll(Pageable pageable) {
        Pageable pageableWithSort = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("startDateTime").ascending());

        Page<OpenMat> page = openMatRepository.findByIsHiddenFalse(pageableWithSort);

        return page.map(OpenMatResponse::from);
    }

    private void validateDateRange(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            throw new ResponseStatusException(BAD_REQUEST, "시작/종료 시간이 필요합니다");
        }
        if (!endDateTime.isAfter(startDateTime)) {
            throw new ResponseStatusException(BAD_REQUEST, "종료 시간은 시작 시간보다 이후여야 합니다");
        }
    }

    private void validateCapacity(Integer maxCapacity) {
        if (maxCapacity != null && maxCapacity < -1) {
            throw new ResponseStatusException(BAD_REQUEST, "정원은 -1 이상이어야 합니다");
        }
    }
}
