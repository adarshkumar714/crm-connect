package com.crmconnect.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findBySubdomain(String subdomain);

    Optional<Tenant> findFirstByActiveTrueOrderByIdAsc();
}

