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
                            :viewerUserId = -1
                            OR s.host.id NOT IN (
                                SELECT blocked.blockedUser.id
                                FROM User viewer
                                JOIN viewer.blockedUserLinks blocked
                                WHERE viewer.id = :viewerUserId
                                  AND blocked.blockedUser.isWithdrawn = false
                            )
                      )
                      AND s.region = COALESCE(:region, s.region)
                      AND s.status = COALESCE(:status, s.status)
                      AND s.startDateTime >= COALESCE(:from, s.startDateTime)
                      AND s.startDateTime <= COALESCE(:to, s.startDateTime)
                      AND (
                            LOWER(s.title) LIKE :keywordPattern
                            OR LOWER(s.instructorName) LIKE :keywordPattern
                            OR LOWER(s.locationName) LIKE :keywordPattern
                            OR LOWER(s.address) LIKE :keywordPattern
                      )
                    """,
            countQuery = """
                    SELECT COUNT(s) FROM Seminar s
                    WHERE s.isHidden = false
                      AND (
                            :viewerUserId = -1
                            OR s.host.id NOT IN (
                                SELECT blocked.blockedUser.id
                                FROM User viewer
                                JOIN viewer.blockedUserLinks blocked
                                WHERE viewer.id = :viewerUserId
                                  AND blocked.blockedUser.isWithdrawn = false
                            )
                      )
                      AND s.region = COALESCE(:region, s.region)
                      AND s.status = COALESCE(:status, s.status)
                      AND s.startDateTime >= COALESCE(:from, s.startDateTime)
                      AND s.startDateTime <= COALESCE(:to, s.startDateTime)
                      AND (
                            LOWER(s.title) LIKE :keywordPattern
                            OR LOWER(s.instructorName) LIKE :keywordPattern
                            OR LOWER(s.locationName) LIKE :keywordPattern
                            OR LOWER(s.address) LIKE :keywordPattern
                      )
                    """
    )
    Page<Seminar> searchVisible(
            @Param("viewerUserId") Long viewerUserId,
            @Param("region") Region region,
            @Param("status") SeminarStatus status,
            @Param("keywordPattern") String keywordPattern,
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
