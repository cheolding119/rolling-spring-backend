package com.rolling.api.domain.user.repository;

import com.rolling.api.domain.user.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByFcmToken(String fcmToken);

    Optional<UserDevice> findByUser_IdAndFcmToken(Long userId, String fcmToken);

    @Query("""
            select ud
            from UserDevice ud
            join ud.user u
            where u.id in :userIds
              and u.isWithdrawn = false
              and u.withdrawalPending = false
            """)
    List<UserDevice> findPushTargetDevicesByUserIds(@Param("userIds") Collection<Long> userIds);

    void deleteAllByUser_Id(Long userId);

    void deleteAllByFcmTokenIn(Collection<String> fcmTokens);
}
