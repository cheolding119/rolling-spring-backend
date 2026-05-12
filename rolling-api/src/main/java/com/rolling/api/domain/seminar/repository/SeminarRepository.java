package com.rolling.api.domain.seminar.repository;

import com.rolling.api.domain.openmat.entity.Region;
import com.rolling.api.domain.seminar.entity.Seminar;
import com.rolling.api.domain.seminar.entity.SeminarStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SeminarRepository extends JpaRepository<Seminar, Long> {

    @EntityGraph(attributePaths = "host")
    @Query(
            value = """
                    SELECT s FROM Seminar s
                    WHERE s.isHidden = false
                      AND (
                            :viewerUserId IS NULL
                            OR s.host.id NOT IN (
                                SELECT blocked.blockedUser.id
                                FROM User viewer
                                JOIN viewer.blockedUserLinks blocked
                                WHERE viewer.id = :viewerUserId
                                  AND blocked.blockedUser.isWithdrawn = false
                            )
                      )
                      AND (:region IS NULL OR s.region = :region)
                      AND (:status IS NULL OR s.status = :status)
                      AND (:from IS NULL OR s.startDateTime >= :from)
                      AND (:to IS NULL OR s.startDateTime <= :to)
                      AND (
                            :keyword IS NULL
                            OR LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(s.instructorName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(s.locationName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(s.address) LIKE LOWER(CONCAT('%', :keyword, '%'))
                      )
                    """,
            countQuery = """
                    SELECT COUNT(s) FROM Seminar s
                    WHERE s.isHidden = false
                      AND (
                            :viewerUserId IS NULL
                            OR s.host.id NOT IN (
                                SELECT blocked.blockedUser.id
                                FROM User viewer
                                JOIN viewer.blockedUserLinks blocked
                                WHERE viewer.id = :viewerUserId
                                  AND blocked.blockedUser.isWithdrawn = false
                            )
                      )
                      AND (:region IS NULL OR s.region = :region)
                      AND (:status IS NULL OR s.status = :status)
                      AND (:from IS NULL OR s.startDateTime >= :from)
                      AND (:to IS NULL OR s.startDateTime <= :to)
                      AND (
                            :keyword IS NULL
                            OR LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(s.instructorName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(s.locationName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(s.address) LIKE LOWER(CONCAT('%', :keyword, '%'))
                      )
                    """
    )
    Page<Seminar> searchVisible(
            @Param("viewerUserId") Long viewerUserId,
            @Param("region") Region region,
            @Param("status") SeminarStatus status,
            @Param("keyword") String keyword,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "host")
    Optional<Seminar> findByIdAndIsHiddenFalse(Long id);

    @EntityGraph(attributePaths = "host")
    Page<Seminar> findByHost_IdAndIsHiddenFalse(Long hostId, Pageable pageable);

    @EntityGraph(attributePaths = "host")
    Page<Seminar> findByHost_IdAndIsHiddenFalseAndStatus(Long hostId, SeminarStatus status, Pageable pageable);

    List<Seminar> findAllByIsHiddenFalseAndStatusNotAndEndDateTimeLessThanEqual(
            SeminarStatus status,
            LocalDateTime endDateTime
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seminar s WHERE s.id = :id AND s.isHidden = false")
    Optional<Seminar> findByIdForUpdate(@Param("id") Long id);
}
