package com.rolling.api.domain.openmat.repository;

import com.rolling.api.domain.openmat.entity.OpenMat;
import com.rolling.api.domain.openmat.entity.OpenMatStatus;
import com.rolling.api.domain.openmat.entity.Region;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OpenMatRepository extends JpaRepository<OpenMat, Long> {

    @EntityGraph(attributePaths = "host")
    Page<OpenMat> findByIsHiddenFalse(Pageable pageable);

    @EntityGraph(attributePaths = "host")
    Page<OpenMat> findByIsHiddenFalseAndRegion(Region region, Pageable pageable);

    @EntityGraph(attributePaths = "host")
    Page<OpenMat> findByIsHiddenFalseAndStatus(OpenMatStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "host")
    Page<OpenMat> findByIsHiddenFalseAndRegionAndStatus(Region region, OpenMatStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "host")
    Page<OpenMat> findByHost_IdAndIsHiddenFalse(Long hostId, Pageable pageable);

    @EntityGraph(attributePaths = "host")
    @Query(
            value = """
                    SELECT o FROM OpenMat o
                    WHERE o.isHidden = false
                      AND (:region IS NULL OR o.region = :region)
                      AND (:status IS NULL OR o.status = :status)
                      AND (
                            :keyword IS NULL
                            OR LOWER(o.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(COALESCE(o.locationName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(COALESCE(o.address, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                      )
                    """,
            countQuery = """
                    SELECT COUNT(o) FROM OpenMat o
                    WHERE o.isHidden = false
                      AND (:region IS NULL OR o.region = :region)
                      AND (:status IS NULL OR o.status = :status)
                      AND (
                            :keyword IS NULL
                            OR LOWER(o.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(COALESCE(o.locationName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(COALESCE(o.address, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                      )
                    """
    )
    Page<OpenMat> searchVisible(
            @Param("region") Region region,
            @Param("status") OpenMatStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    Optional<OpenMat> findByIdAndIsHiddenFalse(Long id);

    List<OpenMat> findAllByIsHiddenFalseAndStatusNotAndEndDateTimeLessThanEqual(
            OpenMatStatus status,
            LocalDateTime endDateTime
    );

    @Query("""
            select o.id as openMatId, count(p) as participantCount
            from OpenMat o
            left join o.participantUids p
            where o.id in :openMatIds
            group by o.id
            """)
    List<OpenMatParticipantCountView> countParticipantsByOpenMatIds(@Param("openMatIds") List<Long> openMatIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OpenMat o WHERE o.id = :id AND o.isHidden = false")
    Optional<OpenMat> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = "host")
    @Query(
            value = "SELECT DISTINCT o FROM OpenMat o JOIN o.participantUids p WHERE p = :userId AND o.isHidden = false",
            countQuery = "SELECT COUNT(DISTINCT o.id) FROM OpenMat o JOIN o.participantUids p WHERE p = :userId AND o.isHidden = false"
    )
    Page<OpenMat> findByParticipantUidsContaining(@Param("userId") Long userId, Pageable pageable);

    interface OpenMatParticipantCountView {
        Long getOpenMatId();

        Long getParticipantCount();
    }
}

