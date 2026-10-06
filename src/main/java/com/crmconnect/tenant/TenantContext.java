package com.crmconnect.tenant;

public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT_ID = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(Long id) {
        CURRENT_TENANT_ID.set(id);
    }

    public static Long getTenantId() {
        return CURRENT_TENANT_ID.get();
    }

    public static void clear() {
        CURRENT_TENANT_ID.remove();
    }
}

