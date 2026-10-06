package com.crmconnect.config;

import com.crmconnect.auth.JwtAuthenticationFilter;
import com.crmconnect.tenant.TenantFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain chain(HttpSecurity h, JwtAuthenticationFilter j, TenantFilter t) throws Exception {
        return h.csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.requestMatchers(
                        "/",
                        "/website",
                        "/website.html",
                        "/enquiry",
                        "/enquiry.html",
                        "/contact",
                        "/company/**",
                        "/company.html",
                        "/crm",
                        "/portal",
                        "/login",
                        "/app",
                        "/index.html",
                        "/team.html",
                        "/favicon.ico",
                        "/css/**",
                        "/js/**",
                        "/api/v1/auth/**",
                        "/api/v1/public/enquiries/**",
                        "/api/v1/superadmin/**",
                        "/platform-admin",
                        "/platform-admin.html",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers("/api/v1/users/**").hasAnyRole("ADMIN", "MANAGER")
                .anyRequest().authenticated())
                .addFilterBefore(j, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(t, JwtAuthenticationFilter.class)
                .build();
    }
}
