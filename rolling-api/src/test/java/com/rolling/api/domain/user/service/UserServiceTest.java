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

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

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

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

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

        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        UserResponse response = userService.updateMe(3L, request);

        assertThat(response.getNickname()).isEqualTo("");
        assertThat(response.getBeltColor()).isEqualTo("BROWN");
    }
}
