package com.rolling.api.domain.user.repository;

import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.entity.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findBySocialIdAndSocialProvider(String socialId, SocialProvider socialProvider);

    Optional<User> findBySocialIdAndSocialProviderAndIsWithdrawnFalse(String socialId, SocialProvider socialProvider);

    Optional<User> findBySocialId(String socialId);

    boolean existsBySocialIdAndSocialProvider(String socialId, SocialProvider socialProvider);

    boolean existsByIdAndIsWithdrawnFalse(Long id);

    Optional<User> findByIdAndIsWithdrawnFalse(Long id);

    List<User> findAllByIdInAndIsWithdrawnFalse(Collection<Long> ids);

    List<User> findAllByAccountStatusAndSuspensionUntilLessThanEqual(AccountStatus accountStatus, LocalDateTime suspensionUntil);

    @Query("""
            select blocked.blockedUser.id
            from User user
            join user.blockedUserLinks blocked
            where user.id = :userId
              and blocked.blockedUser.isWithdrawn = false
            """)
    List<Long> findBlockedUserIdsByUserId(@Param("userId") Long userId);

    List<User> findAllByIsWithdrawnFalseAndWithdrawalPendingTrueAndWithdrawalScheduledAtLessThanEqual(LocalDateTime targetTime);
}
