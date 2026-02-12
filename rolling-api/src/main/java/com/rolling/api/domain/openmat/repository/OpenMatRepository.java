package com.rolling.api.domain.openmat.repository;

import com.rolling.api.domain.openmat.entity.OpenMat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpenMatRepository extends JpaRepository<OpenMat, Long> {

    Page<OpenMat> findByIsHiddenFalse(Pageable pageable);
}
