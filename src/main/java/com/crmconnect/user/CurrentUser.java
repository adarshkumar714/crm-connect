package com.crmconnect.user;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Resolves the authenticated workspace member for role and ownership checks. */
@Component
public class CurrentUser {
    private final UserRepository users;

    public CurrentUser(UserRepository users) { this.users = users; }

    public User get() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Signed-in user not found"));
    }

    public boolean isAdminOrManager() {
        User.Role role = get().getRole();
        return role == User.Role.ADMIN || role == User.Role.MANAGER;
    }

    public void requireManagerAccess() {
        if (!isAdminOrManager()) throw new SecurityException("Only an Admin or Sales Manager can perform this action");
    }
}
