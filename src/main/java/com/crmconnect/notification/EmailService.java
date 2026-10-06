package com.crmconnect.notification;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:av3384729@gmail.com}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends an email confirmation to the customer after they submit an enquiry,
     * and sends an instant lead alert notification to the admin/team inbox.
     */
    @Async
    public void sendEnquiryConfirmation(
            String customerEmail,
            String customerName,
            String companyName,
            String enquiryMessage,
            String phone) {
        if (customerEmail == null || customerEmail.isBlank()) {
            log.warn("Cannot send enquiry confirmation: customer email is empty");
            return;
        }

        String displayName = (companyName != null && !companyName.isBlank()) ? companyName : "CRMConnect";

        // 1. Send confirmation email to Customer
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, displayName);
            helper.setTo(customerEmail);
            helper.setSubject("Thank You for Contacting " + displayName + " - Enquiry Confirmation");

            String htmlBody = buildCustomerEmailTemplate(customerName, displayName, enquiryMessage, phone);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Enquiry confirmation email successfully sent to customer: {}", customerEmail);
        } catch (Exception e) {
            log.error("Failed to send enquiry confirmation email to customer [{}]: {}", customerEmail, e.getMessage());
        }

        // 2. Send instant notification to Admin / Workspace owner
        try {
            MimeMessage adminMsg = mailSender.createMimeMessage();
            MimeMessageHelper adminHelper = new MimeMessageHelper(adminMsg, true, "UTF-8");

            adminHelper.setFrom(fromEmail, "CRMConnect Lead Alert");
            adminHelper.setTo(fromEmail);
            adminHelper.setSubject("🚨 New Lead Enquiry Received: " + customerName + " (" + displayName + ")");

            String adminHtml = buildAdminAlertTemplate(customerName, customerEmail, phone, displayName, enquiryMessage);
            adminHelper.setText(adminHtml, true);

            mailSender.send(adminMsg);
            log.info("Admin notification email successfully sent to: {}", fromEmail);
        } catch (Exception e) {
            log.error("Failed to send admin notification email to [{}]: {}", fromEmail, e.getMessage());
        }
    }

    /**
     * Sends a 6-digit email verification OTP code to a customer before they can submit an enquiry.
     */
    @Async
    public void sendOtpEmail(String customerEmail, String otpCode, String companyName) {
        if (customerEmail == null || customerEmail.isBlank()) {
            log.warn("Cannot send OTP: customer email is empty");
            return;
        }

        String displayName = (companyName != null && !companyName.isBlank()) ? companyName : "CRMConnect";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, displayName);
            helper.setTo(customerEmail);
            helper.setSubject("🔒 " + otpCode + " is your Email Verification Code for " + displayName);

            String htmlBody = buildOtpEmailTemplate(otpCode, displayName);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Email verification OTP successfully sent to: {}", customerEmail);
        } catch (Exception e) {
            log.error("Failed to send verification OTP email to [{}]: {}", customerEmail, e.getMessage());
        }
    }

    private String buildOtpEmailTemplate(String otpCode, String companyName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #1e293b; }
                .container { max-width: 520px; margin: 0 auto; background: #ffffff; border-radius: 12px; border: 1px solid #e2e8f0; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); }
                .header { background: linear-gradient(135deg, #2563eb, #1d4ed8); padding: 28px 24px; text-align: center; color: #ffffff; }
                .header h1 { margin: 0; font-size: 20px; font-weight: 700; }
                .content { padding: 32px 24px; text-align: center; }
                .otp-box { background: #f1f5f9; border: 2px dashed #2563eb; border-radius: 10px; padding: 18px; margin: 24px 0; font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #1d4ed8; }
                .subtext { font-size: 13px; color: #64748b; margin-top: 12px; line-height: 1.5; }
                .footer { background: #f8fafc; padding: 16px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>Email Verification</h1>
                  <p style="margin:4px 0 0; font-size:13px; opacity:0.9;">%s Enquiry Portal</p>
                </div>
                <div class="content">
                  <p style="font-size: 15px; margin:0; color:#0f172a; font-weight:600;">Verification Code (OTP)</p>
                  <p style="font-size: 13px; color:#64748b; margin:6px 0 0;">Use the code below to verify your email address and continue your enquiry:</p>
                  
                  <div class="otp-box">%s</div>

                  <div class="subtext">
                    This verification code is valid for <strong>10 minutes</strong>.<br>
                    If you did not request this code, please ignore this email.
                  </div>
                </div>
                <div class="footer">
                  Protected by %s Security System
                </div>
              </div>
            </body>
            </html>
            """.formatted(companyName, otpCode, companyName);
    }


    private String buildCustomerEmailTemplate(String customerName, String companyName, String enquiryMessage, String phone) {
        String safeName = (customerName != null && !customerName.isBlank()) ? customerName : "Valued Customer";
        String safeMsg = (enquiryMessage != null && !enquiryMessage.isBlank()) ? enquiryMessage : "No additional notes provided.";
        String safePhone = (phone != null && !phone.isBlank()) ? phone : "N/A";
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; border: 1px solid #e2e8f0; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); }
                .header { background: linear-gradient(135deg, #2563eb, #1d4ed8); padding: 32px 24px; text-align: center; color: #ffffff; }
                .header h1 { margin: 0; font-size: 22px; font-weight: 700; letter-spacing: -0.5px; }
                .header p { margin: 6px 0 0; font-size: 13px; opacity: 0.9; }
                .content { padding: 32px 24px; font-size: 14px; line-height: 1.6; }
                .greeting { font-size: 16px; font-weight: 600; margin-bottom: 12px; color: #0f172a; }
                .box { background: #f1f5f9; border-left: 4px solid #2563eb; padding: 16px; border-radius: 6px; margin: 20px 0; font-size: 13.5px; }
                .box-title { font-weight: 700; font-size: 11px; text-transform: uppercase; color: #64748b; margin-bottom: 6px; }
                .timeline { background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 8px; padding: 14px 18px; margin: 20px 0; color: #1e40af; font-size: 13px; }
                .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>Enquiry Received</h1>
                  <p>%s</p>
                </div>
                <div class="content">
                  <div class="greeting">Hello %s,</div>
                  <p>Thank you for reaching out to us! We have received your enquiry and our sales &amp; support team is already reviewing your details.</p>
                  
                  <div class="box">
                    <div class="box-title">Your Enquiry Summary</div>
                    <div><strong>Submitted on:</strong> %s</div>
                    <div style="margin-top: 6px;"><strong>Phone:</strong> %s</div>
                    <div style="margin-top: 6px;"><strong>Message / Requirements:</strong></div>
                    <div style="color: #334155; margin-top: 4px; font-style: italic;">&ldquo;%s&rdquo;</div>
                  </div>

                  <div class="timeline">
                    <strong>What happens next:</strong> A dedicated representative will review your request and get in touch with you via email or phone within <strong>24 business hours</strong>.
                  </div>

                  <p>If you have any urgent queries or updates, simply reply directly to this email.</p>
                  
                  <p style="margin-top: 24px;">Warm regards,<br><strong>%s Sales Team</strong></p>
                </div>
                <div class="footer">
                  This is an automated confirmation sent by %s CRM System.
                </div>
              </div>
            </body>
            </html>
            """.formatted(companyName, safeName, timeStr, safePhone, safeMsg, companyName, companyName);
    }

    private String buildAdminAlertTemplate(String customerName, String customerEmail, String phone, String companyName, String enquiryMessage) {
        String safeName = (customerName != null && !customerName.isBlank()) ? customerName : "N/A";
        String safePhone = (phone != null && !phone.isBlank()) ? phone : "N/A";
        String safeMsg = (enquiryMessage != null && !enquiryMessage.isBlank()) ? enquiryMessage : "N/A";
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: sans-serif; background: #f8fafc; padding: 20px; color: #1e293b; }
                .card { max-width: 580px; margin: 0 auto; background: #fff; border-radius: 10px; border: 1px solid #e2e8f0; padding: 24px; }
                h2 { color: #0f172a; margin-top: 0; font-size: 18px; }
                table { width: 100%; border-collapse: collapse; margin: 16px 0; }
                th, td { text-align: left; padding: 8px 10px; border-bottom: 1px solid #f1f5f9; font-size: 13px; }
                th { color: #64748b; width: 140px; font-weight: 600; }
              </style>
            </head>
            <body>
              <div class="card">
                <h2>New Lead Captured from Public Website</h2>
                <p>A new customer has filled out the public enquiry form.</p>
                <table>
                  <tr><th>Customer Name</th><td><strong>%s</strong></td></tr>
                  <tr><th>Work Email</th><td><a href="mailto:%s">%s</a></td></tr>
                  <tr><th>Phone Number</th><td>%s</td></tr>
                  <tr><th>Target Company</th><td>%s</td></tr>
                  <tr><th>Submission Time</th><td>%s</td></tr>
                  <tr><th>Message / Need</th><td>%s</td></tr>
                </table>
                <p>This lead has also been saved to your CRM <strong>Contacts &amp; Leads</strong> pipeline.</p>
              </div>
            </body>
            </html>
            """.formatted(safeName, customerEmail, customerEmail, safePhone, companyName, timeStr, safeMsg);
    }
}
