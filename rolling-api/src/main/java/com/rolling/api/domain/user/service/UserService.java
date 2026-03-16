package com.rolling.api.domain.user.service;

import com.rolling.api.domain.user.dto.UserResponse;
import com.rolling.api.domain.user.dto.UserUpdateRequest;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.entity.UserDevice;
import com.rolling.api.domain.user.repository.UserDeviceRepository;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
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
    public void registerFcmToken(Long userId, String fcmToken) {
        User user = getActiveUser(userId);
        String normalizedToken = normalizeFcmToken(fcmToken);

        UserDevice userDevice = userDeviceRepository.findByFcmToken(normalizedToken)
                .orElseGet(() -> UserDevice.builder()
                        .fcmToken(normalizedToken)
                        .build());

        userDevice.assignUser(user);
        userDeviceRepository.save(userDevice);
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

    private String normalizeFcmToken(String fcmToken) {
        if (fcmToken == null || fcmToken.isBlank()) {
            throw BusinessException.badRequest("fcmToken은 비어 있을 수 없습니다");
        }
        return fcmToken.trim();
    }
}
