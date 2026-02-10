package com.rolling.api.domain.user.repository;

import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findBySocialIdAndSocialProvider(String socialId, SocialProvider socialProvider);

    Optional<User> findBySocialId(String socialId);

    boolean existsBySocialIdAndSocialProvider(String socialId, SocialProvider socialProvider);
}
