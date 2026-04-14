package com.rolling.api.domain.user.service;

import com.rolling.api.domain.user.dto.AdminUserDetailResponse;
import com.rolling.api.domain.user.dto.AdminUserSummaryResponse;
import com.rolling.api.domain.user.dto.UserSanctionCreateRequest;
import com.rolling.api.domain.user.dto.UserSanctionResponse;
import com.rolling.api.domain.user.entity.AccountStatus;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.entity.UserSanction;
import com.rolling.api.domain.user.entity.UserSanctionType;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.domain.user.repository.UserSanctionRepository;
import com.rolling.api.global.exception.BusinessException;
import com.rolling.api.global.page.PageableUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;

import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private static final Sort DEFAULT_ADMIN_SORT = Sort.by(Sort.Direction.DESC, "createdAt");
    private static final java.util.Set<String> ALLOWED_ADMIN_SORTS = java.util.Set.of(
            "createdAt",
            "updatedAt",
            "nickname",
            "email",
            "accountStatus",
            "suspensionUntil"
    );

    private final UserRepository userRepository;
    private final UserSanctionRepository userSanctionRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<AdminUserSummaryResponse> findUsers(String query, AccountStatus status, Pageable pageable) {
        Pageable normalized = PageableUtils.normalize(pageable, DEFAULT_ADMIN_SORT, ALLOWED_ADMIN_SORTS, 20, 100);
        LocalDateTime now = now();
        Page<User> users = userRepository.findAll(buildSpecification(query, status), normalized);
        Map<Long, LocalDateTime> lastSanctionAtMap = loadLastSanctionAt(users.getContent());

        return users.map(user -> AdminUserSummaryResponse.from(
                user,
                now,
                lastSanctionAtMap.get(user.getId())
        ));
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse findUser(Long userId) {
        User user = getUser(userId);
        LocalDateTime now = now();
        return AdminUserDetailResponse.from(user, now, loadLastSanctionAt(List.of(user)).get(user.getId()));
    }

    @Transactional(readOnly = true)
    public List<UserSanctionResponse> findSanctions(Long userId) {
        getUser(userId);
        return userSanctionRepository.findAllByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(UserSanctionResponse::from)
                .toList();
    }

    @Transactional
    public UserSanctionResponse createSanction(Long adminUserId, Long userId, UserSanctionCreateRequest request) {
        User targetUser = getUser(userId);
        if (adminUserId != null && adminUserId.equals(userId)) {
            throw BusinessException.badRequest("자기 자신은 제재할 수 없습니다");
        }

        UserSanctionType type = requireType(request);
        String reason = requireText(request.getReason(), "제재 사유는 필수입니다");
        String memo = normalizeText(request.getMemo());
        LocalDateTime now = now();
        LocalDateTime endsAt = request.getEndsAt();

        validateRequest(type, endsAt, now);
        closeCurrentSanctionIfPresent(targetUser, adminUserId, now);

        UserSanction sanction = UserSanction.builder()
                .user(targetUser)
                .sanctionType(type)
                .reason(reason)
                .memo(memo)
                .startsAt(now)
                .endsAt(type == UserSanctionType.TEMP_SUSPEND ? endsAt : null)
                .createdByUserId(adminUserId)
                .build();

        userSanctionRepository.save(sanction);
        applySanctionToUser(targetUser, type, reason, endsAt);
        return UserSanctionResponse.from(sanction);
    }

    @Transactional
    public UserSanctionResponse releaseSanction(Long adminUserId, Long userId, Long sanctionId) {
        User targetUser = getUser(userId);
        UserSanction sanction = userSanctionRepository.findByIdAndUser_Id(sanctionId, userId)
                .orElseThrow(() -> BusinessException.notFound("제재를 찾을 수 없습니다"));

        if (sanction.isReleased()) {
            return UserSanctionResponse.from(sanction);
        }

        UserSanction activeSanction = userSanctionRepository.findTopByUser_IdAndReleasedAtIsNullOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> BusinessException.notFound("제재를 찾을 수 없습니다"));

        if (!activeSanction.getId().equals(sanction.getId())) {
            throw BusinessException.badRequest("현재 활성 제재만 해제할 수 있습니다");
        }

        sanction.release(adminUserId, now());
        targetUser.releaseSanction();
        return UserSanctionResponse.from(sanction);
    }

    @Transactional
    @Scheduled(cron = "${admin.sanction.schedule.cron:0 * * * * *}", zone = "${admin.sanction.schedule.zone:Asia/Seoul}")
    public void releaseExpiredSuspensions() {
        LocalDateTime now = now();
        List<User> users = userRepository.findAllByAccountStatusAndSuspensionUntilLessThanEqual(
                AccountStatus.SUSPENDED,
                now
        );

        for (User user : users) {
            userSanctionRepository.findTopByUser_IdAndReleasedAtIsNullOrderByCreatedAtDesc(user.getId())
                    .ifPresent(activeSanction -> activeSanction.release(null, now));
            user.releaseSanction();
        }
    }

    private Specification<User> buildSpecification(String query, AccountStatus status) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("accountStatus"), status));
            }

            if (StringUtils.hasText(query)) {
                String normalized = query.trim().toLowerCase();
                List<Predicate> queryPredicates = new ArrayList<>();
                queryPredicates.add(cb.like(cb.lower(root.get("nickname")), "%" + normalized + "%"));
                queryPredicates.add(cb.like(cb.lower(root.get("email")), "%" + normalized + "%"));
                queryPredicates.add(cb.like(cb.lower(root.get("affiliation")), "%" + normalized + "%"));
                queryPredicates.add(cb.like(cb.lower(root.get("phone")), "%" + normalized + "%"));

                try {
                    Long userId = Long.parseLong(normalized);
                    queryPredicates.add(cb.equal(root.get("id"), userId));
                } catch (NumberFormatException ignored) {
                    // textual query only
                }

                predicates.add(cb.or(queryPredicates.toArray(Predicate[]::new)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void applySanctionToUser(User user, UserSanctionType type, String reason, LocalDateTime endsAt) {
        switch (type) {
            case WARNING -> user.warn(reason);
            case TEMP_SUSPEND -> user.suspend(endsAt, reason);
            case PERMANENT_BAN -> user.ban(reason);
        }
    }

    private void closeCurrentSanctionIfPresent(User user, Long adminUserId, LocalDateTime now) {
        userSanctionRepository.findTopByUser_IdAndReleasedAtIsNullOrderByCreatedAtDesc(user.getId())
                .ifPresent(activeSanction -> activeSanction.release(adminUserId, now));
    }

    private void validateRequest(UserSanctionType type, LocalDateTime endsAt, LocalDateTime now) {
        if (type == UserSanctionType.TEMP_SUSPEND) {
            if (endsAt == null) {
                throw BusinessException.badRequest("일시정지에는 종료 시각이 필요합니다");
            }
            if (!endsAt.isAfter(now)) {
                throw BusinessException.badRequest("일시정지 종료 시각은 현재보다 이후여야 합니다");
            }
            return;
        }

        if (endsAt != null) {
            throw BusinessException.badRequest("경고와 영구정지는 종료 시각을 사용할 수 없습니다");
        }
    }

    private UserSanctionType requireType(UserSanctionCreateRequest request) {
        if (request == null || request.getType() == null) {
            throw BusinessException.badRequest("제재 타입은 필수입니다");
        }
        return request.getType();
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw BusinessException.badRequest(message);
        }
        return value.trim();
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다"));
    }

    private Map<Long, LocalDateTime> loadLastSanctionAt(Collection<User> users) {
        if (users == null || users.isEmpty()) {
            return Map.of();
        }

        List<Long> userIds = users.stream().map(User::getId).toList();
        List<UserSanction> sanctions = userSanctionRepository.findAllByUser_IdInOrderByCreatedAtDesc(userIds);
        Map<Long, LocalDateTime> result = new HashMap<>();
        for (UserSanction sanction : sanctions) {
            result.putIfAbsent(sanction.getUser().getId(), sanction.getCreatedAt());
        }
        return result;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
