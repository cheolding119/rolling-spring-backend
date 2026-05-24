package com.rolling.api.domain.traininglog.repository;

import com.rolling.api.domain.traininglog.entity.UserTrainingLogShareSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserTrainingLogShareSettingRepository extends JpaRepository<UserTrainingLogShareSetting, Long> {

    Optional<UserTrainingLogShareSetting> findByUser_Id(Long userId);

    boolean existsByUser_IdAndShareWithFriendsTrue(Long userId);
}
