package com.crmconnect.tenant;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String companyName;

    @Column(unique = true)
    private String subdomain;

    @Enumerated(EnumType.STRING)
    private Plan subscriptionPlan;

    private LocalDateTime createdAt;
    private boolean active = true;

    public enum Plan {
        FREE,
        BASIC,
        PRO
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getSubdomain() {
        return subdomain;
    }

    public void setSubdomain(String subdomain) {
        this.subdomain = subdomain;
    }

    public Plan getSubscriptionPlan() {
        return subscriptionPlan;
    }

    public void setSubscriptionPlan(Plan subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
