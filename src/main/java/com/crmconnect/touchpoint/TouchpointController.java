package com.crmconnect.touchpoint;

import com.crmconnect.tenant.TenantContext;
import com.crmconnect.user.CurrentUser;
import com.crmconnect.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/touchpoints")
public class TouchpointController {

    private final TouchpointRepository repository;
    private final CurrentUser currentUser;

    public TouchpointController(TouchpointRepository repository, CurrentUser currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    public static class Request {

        @NotNull
        public Touchpoint.Type type;

        @NotBlank
        public String description;

        public Long relatedConnectionId;
        public Long relatedDealId;
        public Long createdByUserId;
        public LocalDateTime dueDate;
        public boolean completed;
    }

    @GetMapping
    public Page<Touchpoint> list(
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {
        User user = currentUser.get();
        return user.getRole() == User.Role.SALES_REP ? repository.findByCreatedByUserId(user.getId(), pageable) : repository.findAll(pageable);
    }

    @GetMapping("/{id}")
    public Touchpoint get(@PathVariable Long id) {
        return requireVisible(getTouchpoint(id));
    }

    @PostMapping
    public Touchpoint create(@Valid @RequestBody Request request) {
        Touchpoint touchpoint = new Touchpoint();
        copyRequest(request, touchpoint);
        touchpoint.setTenantId(TenantContext.getTenantId());
        return repository.save(touchpoint);
    }

    @PutMapping("/{id}")
    public Touchpoint update(
            @PathVariable Long id,
            @Valid @RequestBody Request request) {
        Touchpoint touchpoint = requireVisible(getTouchpoint(id));
        copyRequest(request, touchpoint);
        return repository.save(touchpoint);
    }

    @PatchMapping("/{id}/complete")
    public Touchpoint complete(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean value) {
        Touchpoint touchpoint = requireVisible(getTouchpoint(id));
        touchpoint.setCompleted(value);
        return repository.save(touchpoint);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.delete(requireVisible(getTouchpoint(id)));
    }

    private Touchpoint getTouchpoint(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Touchpoint not found"));
    }

    private void copyRequest(Request request, Touchpoint touchpoint) {
        touchpoint.setType(request.type);
        touchpoint.setDescription(request.description);
        touchpoint.setRelatedConnectionId(request.relatedConnectionId);
        touchpoint.setRelatedDealId(request.relatedDealId);
        touchpoint.setCreatedByUserId(currentUser.get().getId());
        touchpoint.setDueDate(request.dueDate);
        touchpoint.setCompleted(request.completed);
    }

    private Touchpoint requireVisible(Touchpoint touchpoint) {
        User user = currentUser.get();
        if (user.getRole() == User.Role.SALES_REP && !user.getId().equals(touchpoint.getCreatedByUserId()))
            throw new SecurityException("This task belongs to another user");
        return touchpoint;
    }
}
