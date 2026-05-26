package com.rolling.api.domain.user.service;

import com.rolling.api.domain.user.dto.UserResponse;
import com.rolling.api.domain.user.dto.BlockedUserResponse;
import com.rolling.api.domain.user.dto.UserFcmTokenRequest;
import com.rolling.api.domain.user.dto.UserSettingsResponse;
import com.rolling.api.domain.user.dto.UserSettingsUpdateRequest;
import com.rolling.api.domain.user.dto.UserUpdateRequest;
import com.rolling.api.domain.community.dto.CommunityProfileResponse;
import com.rolling.api.domain.community.dto.CommunityProfileUpdateRequest;
import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.entity.UserDevice;
import com.rolling.api.domain.user.entity.UserBlock;
import com.rolling.api.domain.user.repository.UserBlockRepository;
import com.rolling.api.domain.user.repository.UserDeviceRepository;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.security.AdminAccessConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private AdminAccessConfig adminAccessConfig;

    @Mock
    private Clock clock;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-04-14T03:00:00Z"));
    }

    @Test
    @DisplayName("내 정보 조회 응답에 관리자 여부를 포함한다")
    void getMe_includesIsAdmin() {
        User user = User.builder()
                .socialId("social-me")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("admin-user")
                .email("admin@test.com")
                .affiliation("롤링짐 강남")
                .beltColor(BeltColor.BLACK)
                .build();
        ReflectionTestUtils.setField(user, "id", 11L);

        when(userRepository.findByIdAndIsWithdrawnFalse(11L)).thenReturn(Optional.of(user));
        when(adminAccessConfig.isAdmin(11L)).thenReturn(true);

        UserResponse response = userService.getMe(11L);

        assertThat(response.getIsAdmin()).isTrue();
        assertThat(response.getNickname()).isEqualTo("admin-user");
        assertThat(response.getAffiliation()).isEqualTo("롤링짐 강남");
        assertThat(response.getSettings().getPushNotificationEnabled()).isTrue();
    }

    @Test
    @DisplayName("내 정보 수정 시 nickname, affiliation, beltColor를 반영한다")
    void updateMe_updatesNicknameAffiliationAndBeltColor() {
        User user = User.builder()
                .socialId("social-1")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("old")
                .email("user@test.com")
                .affiliation("old-gym")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        UserUpdateRequest request = new UserUpdateRequest();
        ReflectionTestUtils.setField(request, "nickname", "new-nickname");
        ReflectionTestUtils.setField(request, "affiliation", "new-gym");
        ReflectionTestUtils.setField(request, "beltColor", BeltColor.BLUE);

        when(userRepository.findByIdAndIsWithdrawnFalse(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.updateMe(1L, request);

        assertThat(response.getNickname()).isEqualTo("new-nickname");
        assertThat(response.getAffiliation()).isEqualTo("new-gym");
        assertThat(response.getBeltColor()).isEqualTo("BLUE");
    }

    @Test
    @DisplayName("내 정보 수정 요청 필드가 없어도 검증 없이 기존 값을 유지한다")
    void updateMe_withoutFields_keepsCurrentValues() {
        User user = User.builder()
                .socialId("social-2")
                .socialProvider(SocialProvider.KAKAO)
                .nickname("current")
                .email("user2@test.com")
                .affiliation("current-gym")
                .beltColor(BeltColor.PURPLE)
                .build();
        ReflectionTestUtils.setField(user, "id", 2L);

        UserUpdateRequest request = new UserUpdateRequest();

        when(userRepository.findByIdAndIsWithdrawnFalse(2L)).thenReturn(Optional.of(user));

        UserResponse response = userService.updateMe(2L, request);

        assertThat(response.getNickname()).isEqualTo("current");
        assertThat(response.getAffiliation()).isEqualTo("current-gym");
        assertThat(response.getBeltColor()).isEqualTo("PURPLE");
    }

    @Test
    @DisplayName("내 정보 수정에서 빈 문자열 nickname도 검증 없이 반영한다")
    void updateMe_allowsBlankNickname() {
        User user = User.builder()
                .socialId("social-3")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("nickname")
                .email("user3@test.com")
                .affiliation("gym")
                .beltColor(BeltColor.BROWN)
                .build();
        ReflectionTestUtils.setField(user, "id", 3L);

        UserUpdateRequest request = new UserUpdateRequest();
        ReflectionTestUtils.setField(request, "nickname", "");
        ReflectionTestUtils.setField(request, "affiliation", "");

        when(userRepository.findByIdAndIsWithdrawnFalse(3L)).thenReturn(Optional.of(user));

        UserResponse response = userService.updateMe(3L, request);

        assertThat(response.getNickname()).isEqualTo("");
        assertThat(response.getAffiliation()).isEqualTo("");
        assertThat(response.getBeltColor()).isEqualTo("BROWN");
    }

    @Test
    @DisplayName("내 정보 수정에서 affiliation만 전달해도 반영된다")
    void updateMe_updatesOnlyAffiliation() {
        User user = User.builder()
                .socialId("social-3b")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("nickname")
                .email("user3b@test.com")
                .affiliation("old-gym")
                .beltColor(BeltColor.BROWN)
                .build();
        ReflectionTestUtils.setField(user, "id", 33L);

        UserUpdateRequest request = new UserUpdateRequest();
        ReflectionTestUtils.setField(request, "affiliation", "new-gym");

        when(userRepository.findByIdAndIsWithdrawnFalse(33L)).thenReturn(Optional.of(user));

        UserResponse response = userService.updateMe(33L, request);

        assertThat(response.getNickname()).isEqualTo("nickname");
        assertThat(response.getAffiliation()).isEqualTo("new-gym");
        assertThat(response.getBeltColor()).isEqualTo("BROWN");
    }

    @Test
    @DisplayName("커뮤니티 닉네임 조회 시 현재 값만 반환한다")
    void getCommunityProfile_returnsCurrentNickname() {
        User user = User.builder()
                .socialId("social-community-get")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("open-mat-nickname")
                .affiliation("community-gym")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", 50L);
        ReflectionTestUtils.setField(user, "communityNickname", "community-user");

        when(userRepository.findByIdAndIsWithdrawnFalse(50L)).thenReturn(Optional.of(user));

        CommunityProfileResponse response = userService.getCommunityProfile(50L);

        assertThat(response.getCommunityNickname()).isEqualTo("community-user");
    }

    @Test
    @DisplayName("커뮤니티 닉네임 수정 시 trim된 값이 반영된다")
    void updateCommunityProfile_trimsAndUpdatesNickname() {
        User user = User.builder()
                .socialId("social-community-update")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("open-mat-nickname")
                .affiliation("community-gym")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", 51L);

        CommunityProfileUpdateRequest request = new CommunityProfileUpdateRequest();
        ReflectionTestUtils.setField(request, "communityNickname", "  rolling-community  ");

        when(userRepository.findByIdAndIsWithdrawnFalse(51L)).thenReturn(Optional.of(user));

        CommunityProfileResponse response = userService.updateCommunityProfile(51L, request);

        assertThat(user.getCommunityNickname()).isEqualTo("rolling-community");
        assertThat(response.getCommunityNickname()).isEqualTo("rolling-community");
    }

    @Test
    @DisplayName("사용자 설정 수정 시 pushNotificationEnabled를 반영한다")
    void updateSettings_updatesPushNotificationEnabled() {
        User user = User.builder()
                .socialId("social-settings")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("settings-user")
                .email("settings@test.com")
                .affiliation("settings-gym")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", 31L);

        UserSettingsUpdateRequest request = new UserSettingsUpdateRequest();
        ReflectionTestUtils.setField(request, "pushNotificationEnabled", false);

        when(userRepository.findByIdAndIsWithdrawnFalse(31L)).thenReturn(Optional.of(user));

        UserSettingsResponse response = userService.updateSettings(31L, request);

        assertThat(user.getPushNotificationEnabled()).isFalse();
        assertThat(response.getPushNotificationEnabled()).isFalse();
        assertThat(response.getShowOwnReactions()).isTrue();
    }

    @Test
    @DisplayName("사용자 설정 수정 시 showOwnReactions만 단독으로 반영할 수 있다")
    void updateSettings_updatesShowOwnReactionsOnly() {
        User user = User.builder()
                .socialId("social-settings")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("settings-user")
                .email("settings@test.com")
                .affiliation("settings-gym")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", 31L);

        UserSettingsUpdateRequest request = new UserSettingsUpdateRequest();
        ReflectionTestUtils.setField(request, "showOwnReactions", false);

        when(userRepository.findByIdAndIsWithdrawnFalse(31L)).thenReturn(Optional.of(user));

        UserSettingsResponse response = userService.updateSettings(31L, request);

        assertThat(user.getPushNotificationEnabled()).isTrue();
        assertThat(user.getShowOwnReactions()).isFalse();
        assertThat(response.getPushNotificationEnabled()).isTrue();
        assertThat(response.getShowOwnReactions()).isFalse();
    }

    @Test
    @DisplayName("FCM 토큰 등록 시 사용자 디바이스를 생성한다")
    void registerFcmToken_createsUserDevice() {
        User user = User.builder()
                .socialId("social-4")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("push-user")
                .email("user4@test.com")
                .affiliation("push-gym")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", 4L);

        when(userRepository.findByIdAndIsWithdrawnFalse(4L)).thenReturn(Optional.of(user));
        when(userDeviceRepository.findByFcmToken("fcm-token-123")).thenReturn(Optional.empty());
        when(userDeviceRepository.save(any(UserDevice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.registerFcmToken(4L, createFcmRequest("fcm-token-123", "ANDROID", "device-1", "1.0.0"));

        assertThat(user.getDevices()).hasSize(1);
        assertThat(user.getDevices().get(0).getUser()).isEqualTo(user);
        assertThat(user.getDevices().get(0).getFcmToken()).isEqualTo("fcm-token-123");
        assertThat(user.getDevices().get(0).getPlatform()).isEqualTo("ANDROID");
        assertThat(user.getDevices().get(0).getDeviceId()).isEqualTo("device-1");
        assertThat(user.getDevices().get(0).getAppVersion()).isEqualTo("1.0.0");
        assertThat(user.getFcmToken()).isEqualTo("fcm-token-123");
        verify(userDeviceRepository).save(any(UserDevice.class));
    }

    @Test
    @DisplayName("같은 사용자는 여러 FCM 토큰을 등록할 수 있다")
    void registerFcmToken_allowsMultipleTokensPerUser() {
        User user = User.builder()
                .socialId("social-4b")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("push-user")
                .email("user4b@test.com")
                .affiliation("push-gym-2")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", 44L);

        when(userRepository.findByIdAndIsWithdrawnFalse(44L)).thenReturn(Optional.of(user));
        when(userDeviceRepository.findByFcmToken("fcm-token-1")).thenReturn(Optional.empty());
        when(userDeviceRepository.findByFcmToken("fcm-token-2")).thenReturn(Optional.empty());
        when(userDeviceRepository.save(any(UserDevice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.registerFcmToken(44L, createFcmRequest("fcm-token-1", "ANDROID", "device-1", "1.0.0"));
        userService.registerFcmToken(44L, createFcmRequest("fcm-token-2", "ANDROID", "device-2", "1.0.0"));

        assertThat(user.getDevices()).hasSize(2);
        assertThat(user.getDevices()).extracting(UserDevice::getFcmToken)
                .containsExactly("fcm-token-1", "fcm-token-2");
    }

    @Test
    @DisplayName("이미 등록된 토큰은 현재 사용자에게 재연결된다")
    void registerFcmToken_reassignsExistingTokenToCurrentUser() {
        User previousUser = User.builder()
                .socialId("social-prev")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("previous")
                .email("previous@test.com")
                .affiliation("prev-gym")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(previousUser, "id", 40L);

        User currentUser = User.builder()
                .socialId("social-current")
                .socialProvider(SocialProvider.KAKAO)
                .nickname("current")
                .email("current@test.com")
                .affiliation("current-gym")
                .beltColor(BeltColor.BLUE)
                .build();
        ReflectionTestUtils.setField(currentUser, "id", 41L);

        UserDevice existingDevice = UserDevice.builder()
                .user(previousUser)
                .fcmToken("shared-token")
                .build();

        when(userRepository.findByIdAndIsWithdrawnFalse(41L)).thenReturn(Optional.of(currentUser));
        when(userDeviceRepository.findByFcmToken("shared-token")).thenReturn(Optional.of(existingDevice));
        when(userDeviceRepository.save(any(UserDevice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.registerFcmToken(41L, createFcmRequest("shared-token", "IOS", "device-shared", "2.0.0"));

        assertThat(existingDevice.getUser()).isEqualTo(currentUser);
        assertThat(previousUser.getDevices()).isEmpty();
        assertThat(currentUser.getDevices()).containsExactly(existingDevice);
        assertThat(existingDevice.getPlatform()).isEqualTo("IOS");
        assertThat(existingDevice.getDeviceId()).isEqualTo("device-shared");
        assertThat(existingDevice.getAppVersion()).isEqualTo("2.0.0");
    }

    @Test
    @DisplayName("FCM 토큰 삭제 시 현재 사용자에게 연결된 디바이스만 제거한다")
    void unregisterFcmToken_deletesCurrentUserDevice() {
        User user = User.builder()
                .socialId("social-8")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("push-user")
                .email("user8@test.com")
                .affiliation("remove-gym")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", 8L);

        UserDevice userDevice = UserDevice.builder()
                .user(user)
                .fcmToken("remove-token")
                .platform("ANDROID")
                .deviceId("device-remove")
                .appVersion("1.0.1")
                .build();

        when(userRepository.findByIdAndIsWithdrawnFalse(8L)).thenReturn(Optional.of(user));
        when(userDeviceRepository.findByUser_IdAndFcmToken(8L, "remove-token")).thenReturn(Optional.of(userDevice));

        userService.unregisterFcmToken(8L, " remove-token ");

        assertThat(user.getDevices()).isEmpty();
        verify(userDeviceRepository).delete(userDevice);
    }

    @Test
    @DisplayName("FCM 토큰 삭제는 존재하지 않는 토큰에 대해 idempotent하게 동작한다")
    void unregisterFcmToken_isIdempotentWhenTokenMissing() {
        User user = User.builder()
                .socialId("social-9")
                .socialProvider(SocialProvider.KAKAO)
                .nickname("push-user")
                .email("user9@test.com")
                .affiliation("missing-gym")
                .beltColor(BeltColor.BLUE)
                .build();
        ReflectionTestUtils.setField(user, "id", 9L);

        when(userRepository.findByIdAndIsWithdrawnFalse(9L)).thenReturn(Optional.of(user));
        when(userDeviceRepository.findByUser_IdAndFcmToken(9L, "missing-token")).thenReturn(Optional.empty());

        userService.unregisterFcmToken(9L, "missing-token");

        verify(userDeviceRepository, org.mockito.Mockito.never()).delete(any(UserDevice.class));
    }

    @Test
    @DisplayName("삭제했던 FCM 토큰도 다시 등록하면 현재 사용자 디바이스로 재연결된다")
    void registerFcmToken_reconnectsTokenAfterExplicitRemoval() {
        User user = User.builder()
                .socialId("social-10")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("push-user")
                .email("user10@test.com")
                .affiliation("reconnect-gym")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", 10L);

        UserDevice removedDevice = UserDevice.builder()
                .user(user)
                .fcmToken("reconnect-token")
                .platform("ANDROID")
                .deviceId("device-old")
                .appVersion("1.0.0")
                .build();
        removedDevice.assignUser(null);

        when(userRepository.findByIdAndIsWithdrawnFalse(10L)).thenReturn(Optional.of(user));
        when(userDeviceRepository.findByFcmToken("reconnect-token")).thenReturn(Optional.of(removedDevice));
        when(userDeviceRepository.save(any(UserDevice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.registerFcmToken(10L, createFcmRequest(" reconnect-token ", " IOS ", " device-new ", " 2.0.0 "));

        assertThat(removedDevice.getUser()).isEqualTo(user);
        assertThat(user.getDevices()).containsExactly(removedDevice);
        assertThat(removedDevice.getPlatform()).isEqualTo("IOS");
        assertThat(removedDevice.getDeviceId()).isEqualTo("device-new");
        assertThat(removedDevice.getAppVersion()).isEqualTo("2.0.0");
    }

    @Test
    @DisplayName("사용자 차단과 차단 해제가 blockedUsers에 반영된다")
    void blockAndUnblockUser_updatesBlockedUsers() {
        User user = User.builder()
                .socialId("social-5")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("owner")
                .email("owner@test.com")
                .affiliation("owner-gym")
                .beltColor(BeltColor.BLUE)
                .build();
        ReflectionTestUtils.setField(user, "id", 5L);

        User blockedUser = User.builder()
                .socialId("social-6")
                .socialProvider(SocialProvider.KAKAO)
                .nickname("blocked")
                .email("blocked@test.com")
                .affiliation("blocked-gym")
                .beltColor(BeltColor.PURPLE)
                .build();
        ReflectionTestUtils.setField(blockedUser, "id", 6L);

        when(userRepository.findByIdAndIsWithdrawnFalse(5L)).thenReturn(Optional.of(user));
        when(userRepository.findByIdAndIsWithdrawnFalse(6L)).thenReturn(Optional.of(blockedUser));

        userService.blockUser(5L, 6L);
        assertThat(user.getBlockedUsers()).contains(blockedUser);

        userService.unblockUser(5L, 6L);
        assertThat(user.getBlockedUsers()).doesNotContain(blockedUser);
    }

    @Test
    @DisplayName("차단한 사용자 목록 조회는 차단 시각과 사용자 정보를 반환한다")
    void getBlockedUsers_returnsBlockedUsersWithBlockedAt() {
        User viewer = User.builder()
                .socialId("social-11")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("viewer")
                .email("viewer@test.com")
                .affiliation("viewer-gym")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(viewer, "id", 11L);

        User blockedUser = User.builder()
                .socialId("social-12")
                .socialProvider(SocialProvider.KAKAO)
                .nickname("blocked")
                .email("blocked@test.com")
                .affiliation("blocked-gym")
                .beltColor(BeltColor.BLUE)
                .build();
        ReflectionTestUtils.setField(blockedUser, "id", 12L);

        UserBlock userBlock = new UserBlock(viewer, blockedUser, LocalDateTime.of(2026, 4, 14, 13, 0));

        when(userRepository.findByIdAndIsWithdrawnFalse(11L)).thenReturn(Optional.of(viewer));
        when(userBlockRepository.findAllByUser_IdAndBlockedUser_IsWithdrawnFalseOrderByBlockedAtDesc(11L))
                .thenReturn(List.of(userBlock));

        List<BlockedUserResponse> response = userService.getBlockedUsers(11L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getUserId()).isEqualTo(12L);
        assertThat(response.get(0).getNickname()).isEqualTo("blocked");
        assertThat(response.get(0).getBlockedAt()).isNotNull();
    }

    @Test
    @DisplayName("자기 자신은 차단할 수 없다")
    void blockUser_rejectsSelfBlock() {
        assertThatThrownBy(() -> userService.blockUser(7L, 7L))
                .hasMessage("자기 자신은 차단할 수 없습니다");
    }

    private UserFcmTokenRequest createFcmRequest(String fcmToken, String platform, String deviceId, String appVersion) {
        UserFcmTokenRequest request = new UserFcmTokenRequest();
        ReflectionTestUtils.setField(request, "fcmToken", fcmToken);
        ReflectionTestUtils.setField(request, "platform", platform);
        ReflectionTestUtils.setField(request, "deviceId", deviceId);
        ReflectionTestUtils.setField(request, "appVersion", appVersion);
        return request;
    }

}





