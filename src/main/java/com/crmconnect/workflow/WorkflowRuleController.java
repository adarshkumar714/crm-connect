package com.crmconnect.workflow;

import com.crmconnect.tenant.TenantContext;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workflow-rules")
public class WorkflowRuleController {

    private final WorkflowRuleRepository repository;

    public WorkflowRuleController(WorkflowRuleRepository repository) {
        this.repository = repository;
    }

    private WorkflowRule one(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workflow rule not found"));
    }

    @GetMapping
    public List<WorkflowRule> list() {
        return repository.findAll();
    }

    @PostMapping
    public WorkflowRule create(@RequestBody WorkflowRule rule) {
        rule.setId(null);
        rule.setTenantId(TenantContext.getTenantId());
        return repository.save(rule);
    }

    @PutMapping("/{id}")
    public WorkflowRule update(@PathVariable Long id, @RequestBody WorkflowRule rule) {
        rule.setId(id);
        rule.setTenantId(one(id).getTenantId());
        return repository.save(rule);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.delete(one(id));
    }
}

