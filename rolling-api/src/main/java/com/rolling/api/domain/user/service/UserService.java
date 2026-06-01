package com.rolling.api.domain.user.service;

import com.rolling.api.domain.user.dto.UserFcmTokenRequest;
import com.rolling.api.domain.user.dto.BlockedUserResponse;
import com.rolling.api.domain.user.dto.UserResponse;
import com.rolling.api.domain.user.dto.UserSettingsResponse;
import com.rolling.api.domain.user.dto.UserSettingsUpdateRequest;
import com.rolling.api.domain.user.dto.UserUpdateRequest;
import com.rolling.api.domain.community.dto.CommunityProfileResponse;
import com.rolling.api.domain.community.dto.CommunityProfileUpdateRequest;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.entity.UserDevice;
import com.rolling.api.domain.user.repository.UserBlockRepository;
import com.rolling.api.domain.user.repository.UserDeviceRepository;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import com.rolling.api.global.security.AdminAccessConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final UserBlockRepository userBlockRepository;
    private final AdminAccessConfig adminAccessConfig;
    private final Clock clock;

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = getActiveUser(userId);
        return UserResponse.from(user, adminAccessConfig.isAdmin(userId), now());
    }

    @Transactional(readOnly = true)
    public CommunityProfileResponse getCommunityProfile(Long userId) {
        User user = getActiveUser(userId);
        return CommunityProfileResponse.from(user.getCommunityNickname());
    }

    @Transactional
    public UserResponse updateMe(Long userId, UserUpdateRequest request) {
        User user = getActiveUser(userId);

        user.updateNickname(request.getNickname());
        user.updateAffiliation(request.getAffiliation());
        user.updateBeltColor(request.getBeltColor());
        user.updateStripeCount(request.getStripeCount());

        return UserResponse.from(user, adminAccessConfig.isAdmin(userId), now());
    }

    @Transactional
    public CommunityProfileResponse updateCommunityProfile(Long userId, CommunityProfileUpdateRequest request) {
        User user = getActiveUser(userId);
        String communityNickname = normalizeCommunityNickname(request.getCommunityNickname());
        user.updateCommunityNickname(communityNickname);
        return CommunityProfileResponse.from(user.getCommunityNickname());
    }

    @Transactional
    public UserSettingsResponse updateSettings(Long userId, UserSettingsUpdateRequest request) {
        User user = getActiveUser(userId);
        if (request.getPushNotificationEnabled() != null) {
            user.updatePushNotificationEnabled(request.getPushNotificationEnabled());
        }
        if (request.getShowOwnReactions() != null) {
            user.updateShowOwnReactions(request.getShowOwnReactions());
        }
        return UserSettingsResponse.from(user);
    }

    @Transactional
    public void registerFcmToken(Long userId, UserFcmTokenRequest request) {
        User user = getActiveUser(userId);
        String normalizedToken = normalizeRequired(fcmToken(request), "fcmToken은 비어 있을 수 없습니다");
        String platform = normalizeOptional(request.getPlatform());
        String deviceId = normalizeOptional(request.getDeviceId());
        String appVersion = normalizeOptional(request.getAppVersion());

        UserDevice userDevice = userDeviceRepository.findByFcmToken(normalizedToken)
                .orElseGet(() -> UserDevice.builder()
                        .fcmToken(normalizedToken)
                        .platform(platform)
                        .deviceId(deviceId)
                        .appVersion(appVersion)
                        .build());

        userDevice.updateRegistrationInfo(normalizedToken, platform, deviceId, appVersion);
        userDevice.assignUser(user);
        userDeviceRepository.save(userDevice);
    }

    @Transactional
    public void unregisterFcmToken(Long userId, String fcmToken) {
        getActiveUser(userId);
        String normalizedToken = normalizeRequired(fcmToken, "fcmToken은 비어 있을 수 없습니다");
        userDeviceRepository.findByUser_IdAndFcmToken(userId, normalizedToken)
                .ifPresent(this::deleteUserDevice);
    }

    @Transactional
    public void blockUser(Long userId, Long blockedUserId) {
        if (userId.equals(blockedUserId)) {
            throw BusinessException.badRequest("자기 자신은 차단할 수 없습니다");
        }

        User user = getActiveUser(userId);
        User blockedUser = getActiveUser(blockedUserId);
        user.blockUser(blockedUser);
    }

    @Transactional
    public void unblockUser(Long userId, Long blockedUserId) {
        if (userId.equals(blockedUserId)) {
            throw BusinessException.badRequest("자기 자신은 차단 해제할 수 없습니다");
        }

        User user = getActiveUser(userId);
        User blockedUser = getActiveUser(blockedUserId);
        user.unblockUser(blockedUser);
    }

    @Transactional(readOnly = true)
    public List<BlockedUserResponse> getBlockedUsers(Long userId) {
        getActiveUser(userId);
        return userBlockRepository.findAllByUser_IdAndBlockedUser_IsWithdrawnFalseOrderByBlockedAtDesc(userId)
                .stream()
                .map(BlockedUserResponse::from)
                .toList();
    }

    private User getActiveUser(Long userId) {
        return userRepository.findByIdAndIsWithdrawnFalse(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));
    }

    private void deleteUserDevice(UserDevice userDevice) {
        userDevice.assignUser(null);
        userDeviceRepository.delete(userDevice);
    }

    private String fcmToken(UserFcmTokenRequest request) {
        return request == null ? null : request.getFcmToken();
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw BusinessException.badRequest(message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeCommunityNickname(String value) {
        if (!StringUtils.hasText(value)) {
            throw BusinessException.badRequest("communityNickname은 필수입니다");
        }

        String normalized = value.trim();
        if (normalized.length() < 2 || normalized.length() > 20) {
            throw BusinessException.badRequest("communityNickname은 2자 이상 20자 이하여야 합니다");
        }

        return normalized;
    }

    private java.time.LocalDateTime now() {
        return java.time.LocalDateTime.now(clock);
    }
}
