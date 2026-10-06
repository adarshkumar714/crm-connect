package com.crmconnect.tenant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TenantService {

    private final TenantRepository tenants;

    public TenantService(TenantRepository tenants) {
        this.tenants = tenants;
    }

    @Transactional
    public Tenant create(String companyName, String subdomain, Tenant.Plan plan) {
        if (tenants.findBySubdomain(subdomain).isPresent()) {
            throw new IllegalArgumentException("Subdomain is already in use");
        }
        Tenant tenant = new Tenant();
        tenant.setCompanyName(companyName);
        tenant.setSubdomain(subdomain);
        tenant.setSubscriptionPlan(plan);
        tenant.setCreatedAt(LocalDateTime.now());
        tenant.setActive(true);
        return tenants.save(tenant);
    }

    public Tenant findBySubdomain(String subdomain) {
        return tenants.findBySubdomain(subdomain)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
    }
}
