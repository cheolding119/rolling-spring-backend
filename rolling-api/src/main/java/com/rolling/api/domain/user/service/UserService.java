package com.rolling.api.domain.user.service;

import com.rolling.api.domain.user.dto.UserResponse;
import com.rolling.api.domain.user.dto.UserUpdateRequest;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateMe(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        if (request.getNickname() == null && request.getBeltColor() == null) {
            throw BusinessException.badRequest("At least one field (nickname or beltColor) is required");
        }

        if (request.getNickname() != null && request.getNickname().isBlank()) {
            throw BusinessException.badRequest("Nickname cannot be blank");
        }

        user.updateNickname(request.getNickname());
        user.updateBeltColor(request.getBeltColor());

        return UserResponse.from(user);
    }
}
