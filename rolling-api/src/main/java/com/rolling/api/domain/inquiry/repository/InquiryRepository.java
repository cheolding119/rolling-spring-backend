package com.rolling.api.domain.inquiry.repository;

import com.rolling.api.domain.inquiry.entity.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquiry, Long>, JpaSpecificationExecutor<Inquiry> {

    Page<Inquiry> findAllByUser_Id(Long userId, Pageable pageable);

    Optional<Inquiry> findByIdAndUser_Id(Long id, Long userId);
}
