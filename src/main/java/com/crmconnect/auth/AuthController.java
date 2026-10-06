package com.crmconnect.auth;

import com.crmconnect.tenant.Tenant;
import com.crmconnect.tenant.TenantRepository;
import com.crmconnect.user.User;
import com.crmconnect.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final TenantRepository tenants;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(TenantRepository tenants, UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.tenants = tenants;
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public static class Signup {
        @NotBlank
        public String companyName;

        @NotBlank
        public String subdomain;

        @NotBlank
        public String fullName;

        @Email
        @NotBlank
        public String email;

        @Size(min = 8)
        @NotBlank
        public String password;
    }

    public static class Login {
        @Email
        @NotBlank
        public String email;

        @NotBlank
        public String password;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody Signup request) {
        if (tenants.findBySubdomain(request.subdomain).isPresent() || users.findByEmail(request.email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Tenant subdomain or email already exists"));
        }

        Tenant tenant = new Tenant();
        tenant.setCompanyName(request.companyName);
        tenant.setSubdomain(request.subdomain);
        tenant.setSubscriptionPlan(Tenant.Plan.FREE);
        tenant.setCreatedAt(LocalDateTime.now());
        tenants.save(tenant);

        User user = new User();
        user.setTenantId(tenant.getId());
        user.setFullName(request.fullName);
        user.setEmail(request.email);
        user.setPasswordHash(encoder.encode(request.password));
        user.setRole(User.Role.ADMIN);
        user.setCreatedAt(LocalDateTime.now());
        users.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("accessToken", jwt.token(user)));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody Login request) {
        User user = users.findByEmail(request.email)
                .filter(u -> u.isActive() && encoder.matches(request.password, u.getPasswordHash()))
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid credentials"));
        }

        return ResponseEntity.ok(Map.of("accessToken", jwt.token(user)));
    }
}

