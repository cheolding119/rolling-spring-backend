package com.rolling.api.domain.notification.repository;

import com.rolling.api.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findAllByUser_Id(Long userId, Pageable pageable);

    Optional<Notification> findByIdAndUser_Id(Long notificationId, Long userId);
}
