package com.crmconnect.workflow;

import com.crmconnect.connection.Connection;
import com.crmconnect.connection.ConnectionRepository;
import com.crmconnect.notification.EmailService;
import com.crmconnect.touchpoint.Touchpoint;
import com.crmconnect.touchpoint.TouchpointRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/** Rule-based evaluator: matches events and executes automated CRM tasks. */
@Service
public class WorkflowEngine {

    private final WorkflowRuleRepository rules;
    private final TouchpointRepository touchpointRepo;
    private final ConnectionRepository connectionRepo;

    @Autowired(required = false)
    private EmailService emailService;

    public WorkflowEngine(WorkflowRuleRepository r, TouchpointRepository t, ConnectionRepository c) {
        this.rules = r;
        this.touchpointRepo = t;
        this.connectionRepo = c;
    }

    public List<WorkflowRule> evaluate(String event, Map<String, String> values) {
        List<WorkflowRule> matches = new ArrayList<>();
        for (WorkflowRule rule : rules.findByTriggerEventAndActiveTrue(event)) {
            if (rule.getConditionField() == null || rule.getConditionField().isBlank()
                    || Objects.equals(rule.getConditionValue(), values.get(rule.getConditionField()))) {
                matches.add(rule);
            }
        }
        return matches;
    }

    public List<WorkflowRule> evaluateAndExecute(String event, Map<String, String> values,
                                                 Long connectionId, Long dealId, Long tenantId) {
        List<WorkflowRule> matched = evaluate(event, values);
        for (WorkflowRule rule : matched) {
            executeRule(rule, connectionId, dealId, tenantId);
        }
        return matched;
    }

    private void executeRule(WorkflowRule rule, Long connectionId, Long dealId, Long tenantId) {
        try {
            String action = rule.getActionType();
            String val = rule.getActionValue();
            if ("CREATE_TOUCHPOINT".equalsIgnoreCase(action)) {
                Touchpoint t = new Touchpoint();
                t.setTenantId(tenantId);
                t.setType(Touchpoint.Type.CALL);
                t.setDescription(val != null && !val.isBlank() ? val : "Automated task from " + rule.getTriggerEvent());
                t.setRelatedConnectionId(connectionId);
                t.setRelatedDealId(dealId);
                t.setDueDate(LocalDateTime.now().plusDays(1));
                t.setCompleted(false);
                t.setCreatedByUserId(1L);
                touchpointRepo.save(t);
            } else if ("ASSIGN_TO_USER".equalsIgnoreCase(action) && val != null && connectionId != null) {
                try {
                    Long targetUserId = Long.parseLong(val.trim());
                    Optional<Connection> connOpt = connectionRepo.findById(connectionId);
                    if (connOpt.isPresent()) {
                        Connection conn = connOpt.get();
                        conn.setAssignedToUserId(targetUserId);
                        connectionRepo.save(conn);
                    }
                } catch (NumberFormatException ignored) {}
            } else if ("SEND_EMAIL".equalsIgnoreCase(action)) {
                Touchpoint t = new Touchpoint();
                t.setTenantId(tenantId);
                t.setType(Touchpoint.Type.EMAIL);
                t.setDescription("Automated Email: " + (val != null && !val.isBlank() ? val : "System notification"));
                t.setRelatedConnectionId(connectionId);
                t.setRelatedDealId(dealId);
                t.setCompleted(true);
                t.setCreatedByUserId(1L);
                touchpointRepo.save(t);

                if (emailService != null && connectionId != null) {
                    connectionRepo.findById(connectionId).ifPresent(conn -> {
                        if (conn.getEmail() != null && !conn.getEmail().isBlank()) {
                            String fullName = (conn.getFirstName() + " " + conn.getLastName()).trim();
                            emailService.sendEnquiryConfirmation(
                                    conn.getEmail(),
                                    fullName,
                                    conn.getCompanyName(),
                                    val != null ? val : "Automated CRM update",
                                    conn.getPhone()
                            );
                        }
                    });
                }
            }
        } catch (Exception ignored) {
        }
    }
}

