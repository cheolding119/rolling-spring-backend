package com.rolling.api.domain.seminar.repository;

import com.rolling.api.domain.seminar.entity.SeminarApplication;
import com.rolling.api.domain.seminar.entity.SeminarApplicationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SeminarApplicationRepository extends JpaRepository<SeminarApplication, Long> {

    long countBySeminar_IdAndStatus(Long seminarId, SeminarApplicationStatus status);

    @Query("""
            SELECT a.seminar.id AS seminarId, COUNT(a.id) AS applicationCount
            FROM SeminarApplication a
            WHERE a.seminar.id IN :seminarIds
              AND a.status = :status
            GROUP BY a.seminar.id
            """)
    List<SeminarApplicationCountView> countBySeminarIdsAndStatus(
            @Param("seminarIds") Collection<Long> seminarIds,
            @Param("status") SeminarApplicationStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"seminar", "seminar.host", "user"})
    @Query("""
            SELECT a FROM SeminarApplication a
            WHERE a.seminar.id = :seminarId
              AND a.user.id = :userId
            """)
    Optional<SeminarApplication> findBySeminarIdAndUserIdForUpdate(
            @Param("seminarId") Long seminarId,
            @Param("userId") Long userId
    );

    @EntityGraph(attributePaths = {"seminar", "seminar.host", "user"})
    Optional<SeminarApplication> findBySeminar_IdAndUser_Id(Long seminarId, Long userId);

    @EntityGraph(attributePaths = {"seminar", "seminar.host", "user"})
    Optional<SeminarApplication> findBySeminar_IdAndUser_IdAndStatus(
            Long seminarId,
            Long userId,
            SeminarApplicationStatus status
    );

    @EntityGraph(attributePaths = {"seminar", "seminar.host", "user"})
    List<SeminarApplication> findAllBySeminar_IdAndStatus(Long seminarId, SeminarApplicationStatus status);

    @EntityGraph(attributePaths = {"seminar", "seminar.host", "user"})
    Optional<SeminarApplication> findByIdAndSeminar_Id(Long id, Long seminarId);

    @EntityGraph(attributePaths = {"seminar", "seminar.host", "user"})
    @Query(
            value = """
                    SELECT a FROM SeminarApplication a
                    WHERE a.seminar.id = :seminarId
                      AND (:status IS NULL OR a.status = :status)
                    """,
            countQuery = """
                    SELECT COUNT(a) FROM SeminarApplication a
                    WHERE a.seminar.id = :seminarId
                      AND (:status IS NULL OR a.status = :status)
                    """
    )
    Page<SeminarApplication> findBySeminarId(
            @Param("seminarId") Long seminarId,
            @Param("status") SeminarApplicationStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"seminar", "seminar.host", "user"})
    @Query(
            value = """
                    SELECT a FROM SeminarApplication a
                    WHERE a.user.id = :userId
                      AND a.seminar.isHidden = false
                      AND (:status IS NULL OR a.status = :status)
                    """,
            countQuery = """
                    SELECT COUNT(a) FROM SeminarApplication a
                    WHERE a.user.id = :userId
                      AND a.seminar.isHidden = false
                      AND (:status IS NULL OR a.status = :status)
                    """
    )
    Page<SeminarApplication> findMine(
            @Param("userId") Long userId,
            @Param("status") SeminarApplicationStatus status,
            Pageable pageable
    );

    @Query("""
            SELECT a FROM SeminarApplication a
            WHERE a.user.id = :userId
              AND a.seminar.id IN :seminarIds
            """)
    List<SeminarApplication> findAllByUserIdAndSeminarIdIn(
            @Param("userId") Long userId,
            @Param("seminarIds") Collection<Long> seminarIds
    );

    interface SeminarApplicationCountView {
        Long getSeminarId();

        Long getApplicationCount();
    }
}
