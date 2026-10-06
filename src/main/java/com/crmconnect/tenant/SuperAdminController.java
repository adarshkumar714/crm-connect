package com.crmconnect.tenant;

import com.crmconnect.connection.ConnectionRepository;
import com.crmconnect.dealflow.DealFlowRepository;
import com.crmconnect.user.User;
import com.crmconnect.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/superadmin")
public class SuperAdminController {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final ConnectionRepository connectionRepository;
    private final DealFlowRepository dealFlowRepository;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminController(TenantRepository tenantRepository,
                                UserRepository userRepository,
                                ConnectionRepository connectionRepository,
                                DealFlowRepository dealFlowRepository,
                                PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.connectionRepository = connectionRepository;
        this.dealFlowRepository = dealFlowRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPlatformStats() {
        long totalTenants = tenantRepository.count();
        long activeTenants = tenantRepository.findAll().stream().filter(Tenant::isActive).count();
        long totalUsers = userRepository.count();
        long totalConnections = connectionRepository.count();
        long totalDeals = dealFlowRepository.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCompanies", totalTenants);
        stats.put("activeCompanies", activeTenants);
        stats.put("totalUsers", totalUsers);
        stats.put("totalLeads", totalConnections);
        stats.put("totalDeals", totalDeals);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/tenants")
    public ResponseEntity<List<Map<String, Object>>> getAllTenants() {
        List<Tenant> tenants = tenantRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Tenant t : tenants) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId());
            map.put("companyName", t.getCompanyName());
            map.put("subdomain", t.getSubdomain());
            map.put("subscriptionPlan", t.getSubscriptionPlan() != null ? t.getSubscriptionPlan().name() : "FREE");
            map.put("active", t.isActive());
            map.put("createdAt", t.getCreatedAt());

            long userCount = userRepository.countByTenantId(t.getId());
            map.put("userCount", userCount);

            Optional<User> adminUser = userRepository.findFirstByTenantIdAndRoleAndActiveTrue(t.getId(), User.Role.ADMIN);
            map.put("adminEmail", adminUser.map(User::getEmail).orElse("Not Assigned"));
            map.put("adminName", adminUser.map(User::getFullName).orElse("N/A"));

            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @PutMapping("/tenants/{id}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleTenantStatus(@PathVariable Long id) {
        return tenantRepository.findById(id).map(tenant -> {
            tenant.setActive(!tenant.isActive());
            tenantRepository.save(tenant);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("active", tenant.isActive());
            res.put("message", "Company status updated to " + (tenant.isActive() ? "Active" : "Suspended"));
            return ResponseEntity.ok(res);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/tenants/{id}/plan")
    public ResponseEntity<Map<String, Object>> updateTenantPlan(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String planStr = body.get("plan");
        return tenantRepository.findById(id).map(tenant -> {
            try {
                Tenant.Plan newPlan = Tenant.Plan.valueOf(planStr.toUpperCase());
                tenant.setSubscriptionPlan(newPlan);
                tenantRepository.save(tenant);
                Map<String, Object> res = new HashMap<>();
                res.put("success", true);
                res.put("plan", newPlan.name());
                res.put("message", "Subscription plan updated to " + newPlan.name());
                return ResponseEntity.ok(res);
            } catch (Exception e) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("error", "Invalid plan: " + planStr);
                return ResponseEntity.badRequest().body(err);
            }
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/tenants")
    public ResponseEntity<Map<String, Object>> createTenantDirectly(@RequestBody Map<String, String> body) {
        String companyName = body.get("companyName");
        String subdomain = body.get("subdomain");
        String adminName = body.get("adminName");
        String email = body.get("email");
        String password = body.get("password");
        String planStr = body.getOrDefault("plan", "PRO");

        if (tenantRepository.findBySubdomain(subdomain).isPresent()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Subdomain already in use");
            return ResponseEntity.badRequest().body(err);
        }

        Tenant t = new Tenant();
        t.setCompanyName(companyName);
        t.setSubdomain(subdomain.toLowerCase().trim());
        try {
            t.setSubscriptionPlan(Tenant.Plan.valueOf(planStr.toUpperCase()));
        } catch (Exception e) {
            t.setSubscriptionPlan(Tenant.Plan.PRO);
        }
        t.setCreatedAt(LocalDateTime.now());
        t.setActive(true);
        Tenant savedTenant = tenantRepository.save(t);

        User admin = new User();
        admin.setTenantId(savedTenant.getId());
        admin.setFullName(adminName);
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(User.Role.ADMIN);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setActive(true);
        userRepository.save(admin);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("tenantId", savedTenant.getId());
        res.put("message", "Company workspace initialized successfully");
        return ResponseEntity.ok(res);
    }
}