package com.rolling.api.domain.inquiry.repository;

import com.rolling.api.domain.inquiry.entity.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquiry, Long>, JpaSpecificationExecutor<Inquiry> {

    @EntityGraph(attributePaths = "user")
    Page<Inquiry> findAllByUser_Id(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Optional<Inquiry> findByIdAndUser_Id(Long id, Long userId);

    @Override
    @EntityGraph(attributePaths = "user")
    Optional<Inquiry> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "user")
    Page<Inquiry> findAll(Specification<Inquiry> spec, Pageable pageable);
}
