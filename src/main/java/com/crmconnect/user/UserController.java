package com.crmconnect.user;

import com.crmconnect.tenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository users;
    private final PasswordEncoder passwords;

    public UserController(UserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    public static class CreateRequest {
        @NotBlank
        public String fullName;

        @Email
        @NotBlank
        public String email;

        @NotBlank
        public String password;

        @NotNull
        public User.Role role;
    }

    public static class UserView {
        public Long id;
        public String fullName;
        public String email;
        public String role;
        public boolean active;

        UserView(User u) {
            this.id = u.getId();
            this.fullName = u.getFullName();
            this.email = u.getEmail();
            this.role = u.getRole() != null ? u.getRole().name() : "";
            this.active = u.isActive();
        }
    }

    @GetMapping
    public List<UserView> list() {
        Long tenant = TenantContext.getTenantId();
        return users.findAll().stream()
                .filter(u -> tenant.equals(u.getTenantId()))
                .map(UserView::new)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserView create(@Valid @RequestBody CreateRequest r) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User curr = users.findByEmail(currentEmail).orElse(null);
        if (curr == null || curr.getRole() != User.Role.ADMIN) {
            throw new SecurityException("Only an Admin can add team members");
        }
        if (users.findByEmail(r.email).isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }
        User u = new User();
        u.setTenantId(TenantContext.getTenantId());
        u.setFullName(r.fullName);
        u.setEmail(r.email);
        u.setPasswordHash(passwords.encode(r.password));
        u.setRole(r.role);
        u.setCreatedAt(LocalDateTime.now());
        u.setActive(true);
        return new UserView(users.save(u));
    }

    @PatchMapping("/{id}/active")
    public UserView active(@PathVariable Long id, @RequestParam boolean value) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User curr = users.findByEmail(currentEmail).orElse(null);
        if (curr == null || curr.getRole() != User.Role.ADMIN) {
            throw new SecurityException("Only an Admin can change user status");
        }
        User u = users.findById(id)
                .filter(x -> x.getTenantId().equals(TenantContext.getTenantId()))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!value && u.getEmail().equalsIgnoreCase(currentEmail)) {
            throw new IllegalArgumentException("You cannot deactivate your own Admin account");
        }
        u.setActive(value);
        return new UserView(users.save(u));
    }
}
