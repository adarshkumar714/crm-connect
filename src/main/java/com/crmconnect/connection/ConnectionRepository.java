package com.crmconnect.connection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConnectionRepository extends JpaRepository<Connection, Long> {
    Page<Connection> findByAssignedToUserId(Long userId, Pageable pageable);
    long countByAssignedToUserId(Long userId);
}
