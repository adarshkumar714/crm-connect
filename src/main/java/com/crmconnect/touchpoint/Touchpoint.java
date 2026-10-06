package com.crmconnect.touchpoint;

import com.crmconnect.common.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import java.time.LocalDateTime;

@Entity
@Table(name = "touchpoints")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Touchpoint extends BaseEntity {

    private Long tenantId;
    private Long relatedConnectionId;
    private Long relatedDealId;
    private Long createdByUserId;

    @Enumerated(EnumType.STRING)
    private Type type;

    private String description;
    private LocalDateTime dueDate;
    private boolean completed;

    public enum Type {
        CALL,
        EMAIL,
        MEETING,
        NOTE
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getRelatedConnectionId() {
        return relatedConnectionId;
    }

    public void setRelatedConnectionId(Long relatedConnectionId) {
        this.relatedConnectionId = relatedConnectionId;
    }

    public Long getRelatedDealId() {
        return relatedDealId;
    }

    public void setRelatedDealId(Long relatedDealId) {
        this.relatedDealId = relatedDealId;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}

