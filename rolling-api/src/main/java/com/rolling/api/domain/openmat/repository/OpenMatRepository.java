package com.rolling.api.domain.openmat.repository;

import com.rolling.api.domain.openmat.entity.OpenMat;
import com.rolling.api.domain.openmat.entity.Region;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OpenMatRepository extends JpaRepository<OpenMat, Long> {

    Page<OpenMat> findByIsHiddenFalse(Pageable pageable);

    Page<OpenMat> findByIsHiddenFalseAndRegion(Region region, Pageable pageable);

    Optional<OpenMat> findByIdAndIsHiddenFalse(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OpenMat o WHERE o.id = :id AND o.isHidden = false")
    Optional<OpenMat> findByIdForUpdate(@Param("id") Long id);

    @Query(
            value = "SELECT DISTINCT o FROM OpenMat o JOIN o.participantUids p WHERE p = :userId AND o.isHidden = false",
            countQuery = "SELECT COUNT(DISTINCT o.id) FROM OpenMat o JOIN o.participantUids p WHERE p = :userId AND o.isHidden = false"
    )
    Page<OpenMat> findByParticipantUidsContaining(@Param("userId") Long userId, Pageable pageable);
}
