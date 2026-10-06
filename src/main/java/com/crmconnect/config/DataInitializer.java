package com.crmconnect.config;

import com.crmconnect.tenant.Tenant;
import com.crmconnect.tenant.TenantRepository;
import com.crmconnect.user.User;
import com.crmconnect.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(TenantRepository tenantRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Tenant tenant = tenantRepository.findBySubdomain("demo").orElseGet(() -> {
            Tenant t = new Tenant();
            t.setCompanyName("CRMConnect Demo");
            t.setSubdomain("demo");
            t.setSubscriptionPlan(Tenant.Plan.PRO);
            t.setCreatedAt(LocalDateTime.now());
            t.setActive(true);
            return tenantRepository.save(t);
        });

        if (userRepository.findByEmail("akash@gmail.com").isEmpty()) {
            User akash = new User();
            akash.setTenantId(tenant.getId());
            akash.setFullName("Akash Kumar");
            akash.setEmail("akash@gmail.com");
            akash.setPasswordHash(passwordEncoder.encode("Akash@123"));
            akash.setRole(User.Role.ADMIN);
            akash.setCreatedAt(LocalDateTime.now());
            akash.setActive(true);
            userRepository.save(akash);
        }

        if (userRepository.findByEmail("admin@crmconnect.com").isEmpty()) {
            User admin = new User();
            admin.setTenantId(tenant.getId());
            admin.setFullName("Admin User");
            admin.setEmail("admin@crmconnect.com");
            admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
            admin.setRole(User.Role.ADMIN);
            admin.setCreatedAt(LocalDateTime.now());
            admin.setActive(true);
            userRepository.save(admin);
        }
    }
}