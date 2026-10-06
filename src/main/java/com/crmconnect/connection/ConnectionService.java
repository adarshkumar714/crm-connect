package com.crmconnect.connection;

import com.crmconnect.tenant.TenantContext;
import com.crmconnect.user.CurrentUser;
import com.crmconnect.user.User;
import com.crmconnect.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConnectionService {
    private final ConnectionRepository repo;
    private final ConnectionMapper mapper;
    private final CurrentUser currentUser;
    private final UserRepository users;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.crmconnect.workflow.WorkflowEngine workflowEngine;

    public ConnectionService(ConnectionRepository repo, ConnectionMapper mapper, CurrentUser currentUser, UserRepository users) {
        this.repo = repo;
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.users = users;
    }

    public Page<ConnectionDto> list(Pageable pageable) {
        User user = currentUser.get();
        Page<Connection> enquiries = user.getRole() == User.Role.SALES_REP
                ? repo.findByAssignedToUserId(user.getId(), pageable)
                : repo.findAll(pageable);
        return enquiries.map(mapper::toDto);
    }

    public Connection getEntity(Long id) {
        Connection enquiry = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Enquiry not found"));
        requireVisible(enquiry);
        return enquiry;
    }

    public ConnectionDto get(Long id) { return mapper.toDto(getEntity(id)); }

    @Transactional
    public ConnectionDto create(ConnectionRequest request) {
        User user = null;
        try { user = currentUser.get(); } catch (Exception ignored) {}
        Connection enquiry = new Connection();
        copy(request, enquiry);
        enquiry.setTenantId(TenantContext.getTenantId());
        if (user != null && user.getRole() == User.Role.SALES_REP && enquiry.getAssignedToUserId() == null) {
            enquiry.setAssignedToUserId(user.getId());
        }
        Connection saved = repo.save(enquiry);
        if (workflowEngine != null) {
            java.util.Map<String, String> ctx = new java.util.HashMap<>();
            ctx.put("status", saved.getStatus() != null ? saved.getStatus().name() : "");
            ctx.put("source", saved.getSource() != null ? saved.getSource().name() : "");
            workflowEngine.evaluateAndExecute("CONNECTION_CREATED", ctx, saved.getId(), null, saved.getTenantId());
        }
        return mapper.toDto(saved);
    }

    @Transactional
    public ConnectionDto update(Long id, ConnectionRequest request) {
        Connection enquiry = getEntity(id);
        User user = null;
        try { user = currentUser.get(); } catch (Exception ignored) {}
        if (user != null && user.getRole() == User.Role.SALES_REP && !user.getId().equals(enquiry.getAssignedToUserId())) {
            throw new SecurityException("You can only update contacts assigned to you");
        }
        copy(request, enquiry);
        return mapper.toDto(repo.save(enquiry));
    }

    public void delete(Long id) {
        Connection enquiry = getEntity(id);
        User user = null;
        try { user = currentUser.get(); } catch (Exception ignored) {}
        if (user != null && user.getRole() == User.Role.SALES_REP && !user.getId().equals(enquiry.getAssignedToUserId())) {
            throw new SecurityException("You can only delete contacts assigned to you");
        }
        repo.delete(enquiry);
    }

    /** Managers distribute website leads; reps can then work only their own lead. */
    @Transactional
    public ConnectionDto assign(Long id, Long userId) {
        currentUser.requireManagerAccess();
        User rep = users.findById(userId)
                .filter(u -> u.getTenantId().equals(TenantContext.getTenantId()) && u.isActive() && u.getRole() == User.Role.SALES_REP)
                .orElseThrow(() -> new IllegalArgumentException("Choose an active Sales Rep from this workspace"));
        Connection enquiry = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Lead not found"));
        enquiry.setAssignedToUserId(rep.getId());
        return mapper.toDto(repo.save(enquiry));
    }

    public java.util.List<RepView> assignableReps() {
        currentUser.requireManagerAccess();
        return users.findAll().stream()
                .filter(u -> u.getTenantId().equals(TenantContext.getTenantId()) && u.isActive() && u.getRole() == User.Role.SALES_REP)
                .map(RepView::new).toList();
    }

    public record RepView(Long id, String fullName, String email) {
        RepView(User user) { this(user.getId(), user.getFullName(), user.getEmail()); }
    }

    private void requireVisible(Connection enquiry) {
        User user = currentUser.get();
        if (user.getRole() == User.Role.SALES_REP && !user.getId().equals(enquiry.getAssignedToUserId())) {
            throw new SecurityException("This enquiry is not assigned to you");
        }
    }

    private void copy(ConnectionRequest request, Connection enquiry) {
        enquiry.setFirstName(request.getFirstName());
        enquiry.setLastName(request.getLastName());
        enquiry.setEmail(request.getEmail());
        enquiry.setPhone(request.getPhone());
        enquiry.setCompanyName(request.getCompanyName());
        enquiry.setSource(request.getSource());
        enquiry.setStatus(request.getStatus());
        enquiry.setAssignedToUserId(request.getAssignedToUserId());
    }
}
