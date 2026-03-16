package com.rolling.api.domain.user.repository;

import com.rolling.api.domain.user.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByFcmToken(String fcmToken);

    List<UserDevice> findAllByUser_IdIn(Collection<Long> userIds);

    void deleteAllByUser_Id(Long userId);

    void deleteAllByFcmTokenIn(Collection<String> fcmTokens);
}
