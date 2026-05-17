package com.rolling.api.domain.notification.service;

import com.rolling.api.domain.notification.dto.NotificationBadgeResponse;
import com.rolling.api.domain.notification.dto.NotificationResponse;
import com.rolling.api.domain.notification.entity.Notification;
import com.rolling.api.domain.notification.model.PushNotificationCommand;
import com.rolling.api.domain.notification.repository.NotificationRepository;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveNotificationsForUsers(Collection<Long> userIds, PushNotificationCommand command) {
        List<Long> normalizedUserIds = normalizeUserIds(userIds);
        if (normalizedUserIds.isEmpty()) {
            log.info("Skipping notification inbox save because target userIds are empty. type={}", command.type());
            return;
        }

        String route = requireText(command.data().get("route"), "알림 route 는 비어 있을 수 없습니다");
        String title = requireText(command.title(), "알림 제목은 비어 있을 수 없습니다");
        String body = requireText(command.body(), "알림 본문은 비어 있을 수 없습니다");
        Long targetId = requireTargetId(command.targetId());

        Map<Long, User> activeUsersById = userRepository.findAllByIdInAndIsWithdrawnFalse(normalizedUserIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<Notification> notifications = normalizedUserIds.stream()
                .map(activeUsersById::get)
                .filter(Objects::nonNull)
                .map(user -> Notification.builder()
                        .user(user)
                        .type(command.type())
                        .targetId(targetId)
                        .route(route)
                        .title(title)
                        .body(body)
                        .build())
                .toList();

        if (notifications.isEmpty()) {
            log.warn(
                    "Skipping notification inbox save because no active users were found. type={}, targetUserIds={}",
                    command.type(),
                    normalizedUserIds
            );
            return;
        }

        notificationRepository.saveAll(notifications);
        log.info(
                "Saved notification inbox entries. type={}, targetId={}, userCount={}, route={}",
                command.type(),
                targetId,
                notifications.size(),
                route
        );
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> findNotifications(Long userId, Pageable pageable) {
        requireActiveUser(userId);
        Pageable pageableWithSort = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending());

        return notificationRepository.findAllByUser_Id(userId, pageableWithSort)
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public NotificationBadgeResponse getBadge(Long userId) {
        requireActiveUser(userId);

        return NotificationBadgeResponse.builder()
                .unreadCount(notificationRepository.countByUser_IdAndReadAtIsNull(userId))
                .build();
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        requireActiveUser(userId);

        Notification notification = notificationRepository.findByIdAndUser_Id(notificationId, userId)
                .orElseThrow(() -> BusinessException.notFound("알림을 찾을 수 없습니다"));

        notification.markAsRead(LocalDateTime.now(clock));
    }

    private void requireActiveUser(Long userId) {
        if (!userRepository.existsByIdAndIsWithdrawnFalse(userId)) {
            throw BusinessException.notFound("사용자를 찾을 수 없습니다");
        }
    }

    private List<Long> normalizeUserIds(Collection<Long> userIds) {
        if (userIds == null) {
            return List.of();
        }

        return userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Long requireTargetId(Long targetId) {
        if (targetId == null) {
            throw BusinessException.badRequest("알림 targetId 는 비어 있을 수 없습니다");
        }
        return targetId;
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw BusinessException.badRequest(message);
        }
        return value.trim();
    }
}
