package com.crmconnect.tenant;

import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantFilter extends OncePerRequestFilter {

    private final EntityManager entityManager;

    public TenantFilter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Object id = request.getAttribute("tenantId");
            if (id instanceof Long tenantId) {
                TenantContext.setTenantId(tenantId);
                entityManager.unwrap(Session.class)
                        .enableFilter("tenantFilter")
                        .setParameter("tenantId", tenantId);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}

