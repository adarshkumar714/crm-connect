package com.crmconnect.dealflow;

import com.crmconnect.connection.Connection;
import com.crmconnect.connection.ConnectionRepository;
import com.crmconnect.tenant.TenantContext;
import com.crmconnect.user.CurrentUser;
import com.crmconnect.user.User;
import com.crmconnect.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/deal-flow")
public class DealFlowController {

    private final DealFlowRepository repository;
    private final ConnectionRepository connections;
    private final UserRepository users;
    private final CurrentUser currentUser;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.crmconnect.workflow.WorkflowEngine workflowEngine;

    public DealFlowController(DealFlowRepository repository, ConnectionRepository connections, UserRepository users, CurrentUser currentUser) {
        this.repository = repository;
        this.connections = connections;
        this.users = users;
        this.currentUser = currentUser;
    }

    public static class Request {

        @NotBlank
        public String title;

        @NotNull
        public Long connectionId;

        @NotNull
        @DecimalMin("0.0")
        public BigDecimal amount;

        public DealFlow.Stage stage = DealFlow.Stage.NEW;
        public Long assignedToUserId;
        public LocalDate expectedCloseDate;
    }

    @GetMapping
    public Page<DealFlow> list(
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {
        User user = currentUser.get();
        return user.getRole() == User.Role.SALES_REP
                ? repository.findByAssignedToUserId(user.getId(), pageable)
                : repository.findAll(pageable);
    }

    @GetMapping("/{id}")
    public DealFlow get(@PathVariable Long id) {
        return requireVisible(getDeal(id));
    }

    @PostMapping
    public DealFlow create(@Valid @RequestBody Request request) {
        User user = currentUser.get();
        DealFlow deal = new DealFlow();
        copyRequest(request, deal);
        deal.setTenantId(TenantContext.getTenantId());
        if (user.getRole() == User.Role.SALES_REP && deal.getAssignedToUserId() == null) {
            deal.setAssignedToUserId(user.getId());
        }
        DealFlow saved = repository.save(deal);

        // Automatically sync assigned sales rep to connection
        if (saved.getAssignedToUserId() != null && saved.getConnectionId() != null) {
            connections.findById(saved.getConnectionId()).ifPresent(conn -> {
                conn.setAssignedToUserId(saved.getAssignedToUserId());
                if (conn.getStatus() == Connection.Status.LEAD) {
                    conn.setStatus(Connection.Status.QUALIFIED);
                }
                connections.save(conn);
            });
        }

        if (workflowEngine != null) {
            java.util.Map<String, String> ctx = new java.util.HashMap<>();
            ctx.put("stage", saved.getStage() != null ? saved.getStage().name() : "");
            workflowEngine.evaluateAndExecute("DEAL_STAGE_CHANGED", ctx, saved.getConnectionId(), saved.getId(), saved.getTenantId());
        }
        return saved;
    }

    @PutMapping("/{id}")
    public DealFlow update(
            @PathVariable Long id,
            @Valid @RequestBody Request request) {
        DealFlow deal = requireVisible(getDeal(id));
        copyRequest(request, deal);
        DealFlow saved = repository.save(deal);

        if (saved.getAssignedToUserId() != null && saved.getConnectionId() != null) {
            connections.findById(saved.getConnectionId()).ifPresent(conn -> {
                conn.setAssignedToUserId(saved.getAssignedToUserId());
                connections.save(conn);
            });
        }
        return saved;
    }

    @PatchMapping("/{id}/stage")
    public DealFlow updateStage(
            @PathVariable Long id,
            @RequestParam DealFlow.Stage value) {
        DealFlow deal = requireVisible(getDeal(id));
        deal.setStage(value);
        DealFlow saved = repository.save(deal);
        if (workflowEngine != null) {
            java.util.Map<String, String> ctx = new java.util.HashMap<>();
            ctx.put("stage", saved.getStage() != null ? saved.getStage().name() : "");
            workflowEngine.evaluateAndExecute("DEAL_STAGE_CHANGED", ctx, saved.getConnectionId(), saved.getId(), saved.getTenantId());
        }
        return saved;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        DealFlow deal = requireVisible(getDeal(id));
        repository.delete(deal);
    }

    @PatchMapping("/{id}/assign")
    public DealFlow assign(@PathVariable Long id, @RequestParam Long userId) {
        currentUser.requireManagerAccess();
        User rep = users.findById(userId)
                .filter(u -> u.getTenantId().equals(TenantContext.getTenantId()) && u.isActive() && u.getRole() == User.Role.SALES_REP)
                .orElseThrow(() -> new IllegalArgumentException("Choose an active Sales Rep from this workspace"));
        DealFlow deal = getDeal(id);
        deal.setAssignedToUserId(rep.getId());
        Connection enquiry = connections.findById(deal.getConnectionId())
                .orElseThrow(() -> new IllegalArgumentException("Enquiry for this deal was not found"));
        enquiry.setAssignedToUserId(rep.getId());
        connections.save(enquiry);
        return repository.save(deal);
    }

    @GetMapping("/assignable-reps")
    public java.util.List<RepView> assignableReps() {
        currentUser.requireManagerAccess();
        return users.findAll().stream().filter(u -> u.getTenantId().equals(TenantContext.getTenantId()) && u.isActive() && u.getRole() == User.Role.SALES_REP).map(RepView::new).toList();
    }

    public record RepView(Long id, String fullName, String email) { RepView(User u) { this(u.getId(), u.getFullName(), u.getEmail()); } }

    private DealFlow getDeal(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Deal Flow not found"));
    }

    private DealFlow requireVisible(DealFlow deal) {
        User user = currentUser.get();
        if (user.getRole() == User.Role.SALES_REP && !user.getId().equals(deal.getAssignedToUserId()))
            throw new SecurityException("This deal is not assigned to you");
        return deal;
    }

    private void copyRequest(Request request, DealFlow deal) {
        deal.setTitle(request.title);
        deal.setConnectionId(request.connectionId);
        deal.setAmount(request.amount);
        deal.setStage(request.stage);
        deal.setAssignedToUserId(request.assignedToUserId);
        deal.setExpectedCloseDate(request.expectedCloseDate);
    }
}
