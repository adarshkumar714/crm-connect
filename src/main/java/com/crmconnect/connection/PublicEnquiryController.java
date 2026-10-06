package com.crmconnect.connection;

import com.crmconnect.notification.EmailService;
import com.crmconnect.tenant.Tenant;
import com.crmconnect.tenant.TenantRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** Public company sites create a Lead and trigger confirmation emails. Requires OTP Email Verification. */
@RestController
@RequestMapping("/api/v1/public/enquiries")
public class PublicEnquiryController {

    private final TenantRepository tenants;
    private final ConnectionRepository connections;
    private final EmailService emailService;

    // In-memory OTP Cache (email -> OtpData)
    private final Map<String, OtpData> otpCache = new ConcurrentHashMap<>();

    public PublicEnquiryController(
            TenantRepository tenants,
            ConnectionRepository connections,
            EmailService emailService) {
        this.tenants = tenants;
        this.connections = connections;
        this.emailService = emailService;
    }

    private static class OtpData {
        final String otp;
        final LocalDateTime expiry;
        boolean verified;

        OtpData(String otp, LocalDateTime expiry) {
            this.otp = otp;
            this.expiry = expiry;
            this.verified = false;
        }
    }

    public static class EnquiryRequest {
        @NotBlank
        public String firstName;

        @NotBlank
        public String lastName;

        @Email
        @NotBlank
        public String email;

        public String phone;
        public String companyName;
        public String message;
    }

    public static class SendOtpRequest {
        @Email
        @NotBlank
        public String email;
        public String subdomain;
    }

    public static class VerifyOtpRequest {
        @Email
        @NotBlank
        public String email;

        @NotBlank
        public String otp;
    }

    /** Sends a 6-digit OTP code to the requested work email. */
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        String cleanEmail = request.email.trim().toLowerCase();

        String subdomain = (request.subdomain != null && !request.subdomain.isBlank()) ? request.subdomain : "default";
        Tenant tenant = ("default".equalsIgnoreCase(subdomain)
                ? tenants.findFirstByActiveTrueOrderByIdAsc()
                : tenants.findBySubdomain(subdomain).filter(Tenant::isActive))
                .orElse(null);

        String companyName = (tenant != null) ? tenant.getCompanyName() : "CRMConnect";

        // Generate 6-digit OTP
        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 1000000));
        otpCache.put(cleanEmail, new OtpData(otp, LocalDateTime.now().plusMinutes(10)));

        // Send OTP via email
        emailService.sendOtpEmail(cleanEmail, otp, companyName);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Verification code (OTP) sent to " + cleanEmail + ". Please check your inbox/spam."
        ));
    }

    /** Verifies the 6-digit OTP code entered by the user. */
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        String cleanEmail = request.email.trim().toLowerCase();
        OtpData data = otpCache.get(cleanEmail);

        if (data == null || data.expiry.isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Verification code expired or invalid. Please click Send OTP again."
            ));
        }

        if (!data.otp.equals(request.otp.trim())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Incorrect OTP verification code. Please check your email and try again."
            ));
        }

        data.verified = true;
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Email verified successfully!"
        ));
    }

    @PostMapping("/submit/{subdomain}")
    public ResponseEntity<?> submit(@PathVariable String subdomain, @Valid @RequestBody EnquiryRequest request) {
        return processSubmit(subdomain, request);
    }

    @PostMapping("/{subdomain}")
    public ResponseEntity<?> legacySubmit(@PathVariable String subdomain, @Valid @RequestBody EnquiryRequest request) {
        if ("send-otp".equalsIgnoreCase(subdomain) || "verify-otp".equalsIgnoreCase(subdomain)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid endpoint mapping"));
        }
        return processSubmit(subdomain, request);
    }

    private ResponseEntity<?> processSubmit(String subdomain, EnquiryRequest request) {
        String cleanEmail = request.email.trim().toLowerCase();
        OtpData otpData = otpCache.get(cleanEmail);

        // Require verified email before accepting enquiry
        if (otpData == null || !otpData.verified) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Please verify your email address via OTP before submitting the enquiry."
            ));
        }

        Tenant tenant = ("default".equalsIgnoreCase(subdomain)
                ? tenants.findFirstByActiveTrueOrderByIdAsc()
                : tenants.findBySubdomain(subdomain).filter(Tenant::isActive))
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        Connection lead = new Connection();
        lead.setTenantId(tenant.getId());
        lead.setFirstName(request.firstName);
        lead.setLastName(request.lastName);
        lead.setEmail(cleanEmail);
        lead.setPhone(request.phone);
        lead.setCompanyName(request.companyName);
        lead.setSource(Connection.Source.WEBSITE);
        lead.setStatus(Connection.Status.LEAD);

        connections.save(lead);

        // Clear OTP data after successful enquiry creation
        otpCache.remove(cleanEmail);

        // Send confirmation email to customer & alert to admin
        String customerFullName = (request.firstName + " " + request.lastName).trim();
        emailService.sendEnquiryConfirmation(
                cleanEmail,
                customerFullName,
                tenant.getCompanyName(),
                request.message,
                request.phone
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Thank you. Your enquiry has been received and a confirmation email has been sent to " + cleanEmail,
                "leadId", lead.getId()
        ));
    }

    /** Single-company deployment config used by the public homepage. */
    @GetMapping("/company")
    public Map<String, String> company() {
        Tenant tenant = tenants.findFirstByActiveTrueOrderByIdAsc()
                .orElseThrow(() -> new IllegalArgumentException("Company setup is not complete"));
        return Map.of("companyName", tenant.getCompanyName(), "subdomain", tenant.getSubdomain());
    }
}


