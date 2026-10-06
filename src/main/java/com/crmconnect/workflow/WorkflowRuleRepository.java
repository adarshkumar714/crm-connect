package com.crmconnect.workflow;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkflowRuleRepository extends JpaRepository<WorkflowRule, Long> {

    List<WorkflowRule> findByTriggerEventAndActiveTrue(String triggerEvent);
}

