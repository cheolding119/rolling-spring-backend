package com.rolling.api.domain.user.service;

import com.rolling.api.domain.user.dto.UserResponse;
import com.rolling.api.domain.user.dto.UserUpdateRequest;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.entity.UserDevice;
import com.rolling.api.domain.user.dto.UserFcmTokenRequest;
import com.rolling.api.domain.user.repository.UserDeviceRepository;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = getActiveUser(userId);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateMe(Long userId, UserUpdateRequest request) {
        User user = getActiveUser(userId);

        user.updateNickname(request.getNickname());
        user.updateBeltColor(request.getBeltColor());

        return UserResponse.from(user);
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
}
