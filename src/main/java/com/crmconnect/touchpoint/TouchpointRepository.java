package com.crmconnect.touchpoint;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TouchpointRepository extends JpaRepository<Touchpoint, Long> {

    long countByCompletedFalse();

    Page<Touchpoint> findByCreatedByUserId(Long userId, Pageable pageable);

    long countByCreatedByUserIdAndCompletedFalse(Long userId);
}

