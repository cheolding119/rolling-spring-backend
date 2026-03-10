package com.rolling.api.domain.user.service;

import com.rolling.api.domain.user.dto.UserResponse;
import com.rolling.api.domain.user.dto.UserUpdateRequest;
import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("내 정보 수정 시 nickname, beltColor를 반영한다")
    void updateMe_updatesNicknameAndBeltColor() {
        User user = User.builder()
                .socialId("social-1")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("old")
                .email("user@test.com")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        UserUpdateRequest request = new UserUpdateRequest();
        ReflectionTestUtils.setField(request, "nickname", "new-nickname");
        ReflectionTestUtils.setField(request, "beltColor", BeltColor.BLUE);

        when(userRepository.findByIdAndIsWithdrawnFalse(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.updateMe(1L, request);

        assertThat(response.getNickname()).isEqualTo("new-nickname");
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
                .beltColor(BeltColor.PURPLE)
                .build();
        ReflectionTestUtils.setField(user, "id", 2L);

        UserUpdateRequest request = new UserUpdateRequest();

        when(userRepository.findByIdAndIsWithdrawnFalse(2L)).thenReturn(Optional.of(user));

        UserResponse response = userService.updateMe(2L, request);

        assertThat(response.getNickname()).isEqualTo("current");
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
                .beltColor(BeltColor.BROWN)
                .build();
        ReflectionTestUtils.setField(user, "id", 3L);

        UserUpdateRequest request = new UserUpdateRequest();
        ReflectionTestUtils.setField(request, "nickname", "");

        when(userRepository.findByIdAndIsWithdrawnFalse(3L)).thenReturn(Optional.of(user));

        UserResponse response = userService.updateMe(3L, request);

        assertThat(response.getNickname()).isEqualTo("");
        assertThat(response.getBeltColor()).isEqualTo("BROWN");
    }

    @Test
    @DisplayName("FCM 토큰 등록 시 사용자 토큰을 갱신한다")
    void registerFcmToken_updatesUserToken() {
        User user = User.builder()
                .socialId("social-4")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("push-user")
                .email("user4@test.com")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", 4L);

        when(userRepository.findByIdAndIsWithdrawnFalse(4L)).thenReturn(Optional.of(user));

        userService.registerFcmToken(4L, "fcm-token-123");

        assertThat(user.getFcmToken()).isEqualTo("fcm-token-123");
    }

    @Test
    @DisplayName("사용자 차단과 차단 해제가 blockedUsers에 반영된다")
    void blockAndUnblockUser_updatesBlockedUsers() {
        User user = User.builder()
                .socialId("social-5")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("owner")
                .email("owner@test.com")
                .beltColor(BeltColor.BLUE)
                .build();
        ReflectionTestUtils.setField(user, "id", 5L);

        User blockedUser = User.builder()
                .socialId("social-6")
                .socialProvider(SocialProvider.KAKAO)
                .nickname("blocked")
                .email("blocked@test.com")
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
    @DisplayName("자기 자신은 차단할 수 없다")
    void blockUser_rejectsSelfBlock() {
        assertThatThrownBy(() -> userService.blockUser(7L, 7L))
                .hasMessage("자기 자신은 차단할 수 없습니다");
    }
}
