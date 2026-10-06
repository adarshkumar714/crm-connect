package com.crmconnect.connection;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Serves public websites and CRM workspace on separate dedicated URLs. */
@Controller
public class PublicWebsiteController {

    /** Main Public Customer Website & Enquiry Page */
    @GetMapping({"/", "/website", "/home", "/enquiry", "/contact"})
    public String mainWebsite() {
        return "forward:/company.html";
    }

    /** Public Company / Subdomain URL */
    @GetMapping("/company/{subdomain}")
    public String companyWebsite(@PathVariable String subdomain) {
        return "forward:/company.html";
    }

    /** CRM Portal / Staff Login & Workspace */
    @GetMapping({"/crm", "/portal", "/login", "/app"})
    public String crmPortal() {
        return "forward:/index.html";
    }
}
