package com.rolling.api.domain.user.repository;

import com.rolling.api.domain.user.entity.UserSanction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserSanctionRepository extends JpaRepository<UserSanction, Long> {

    List<UserSanction> findAllByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<UserSanction> findByIdAndUser_Id(Long id, Long userId);

    Optional<UserSanction> findTopByUser_IdAndReleasedAtIsNullOrderByCreatedAtDesc(Long userId);

    List<UserSanction> findAllByUser_IdAndSanctionTypeAndReleasedAtIsNullAndEndsAtLessThanEqualOrderByCreatedAtAsc(
            Long userId,
            String sanctionType,
            LocalDateTime endsAt
    );

    List<UserSanction> findAllBySanctionTypeAndReleasedAtIsNullAndEndsAtLessThanEqualOrderByCreatedAtAsc(
            String sanctionType,
            LocalDateTime endsAt
    );

    List<UserSanction> findAllByUser_IdAndSanctionTypeAndReleasedAtIsNullOrderByCreatedAtDesc(Long userId, String sanctionType);

    List<UserSanction> findAllByUser_IdInOrderByCreatedAtDesc(Collection<Long> userIds);
}
