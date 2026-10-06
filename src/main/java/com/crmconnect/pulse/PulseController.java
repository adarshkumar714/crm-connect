package com.crmconnect.pulse;

import com.crmconnect.connection.ConnectionRepository;
import com.crmconnect.dealflow.DealFlow;
import com.crmconnect.dealflow.DealFlowRepository;
import com.crmconnect.touchpoint.TouchpointRepository;
import com.crmconnect.user.CurrentUser;
import com.crmconnect.user.User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pulse")
public class PulseController {

    private final ConnectionRepository connections;
    private final DealFlowRepository deals;
    private final TouchpointRepository touchpoints;
    private final CurrentUser currentUser;

    public PulseController(
            ConnectionRepository connections,
            DealFlowRepository deals,
            TouchpointRepository touchpoints,
            CurrentUser currentUser) {
        this.connections = connections;
        this.deals = deals;
        this.touchpoints = touchpoints;
        this.currentUser = currentUser;
    }

    @GetMapping("/summary")
    public Map<String, Long> summary() {
        LocalDateTime start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        User user = currentUser.get();

        if (user.getRole() == User.Role.SALES_REP) {
            return Map.of(
                    "totalConnections", connections.countByAssignedToUserId(user.getId()),
                    "openDeals", deals.countByAssignedToUserIdAndStageNotIn(user.getId(), new DealFlow.Stage[]{DealFlow.Stage.WON, DealFlow.Stage.LOST}),
                    "dealsWonThisMonth", deals.countByAssignedToUserIdAndStageAndUpdatedAtBetween(user.getId(), DealFlow.Stage.WON, start, LocalDateTime.now()),
                    "pendingTouchpoints", touchpoints.countByCreatedByUserIdAndCompletedFalse(user.getId())
            );
        }

        return Map.of(
                "totalConnections", connections.count(),
                "openDeals", deals.countByStageNotIn(new DealFlow.Stage[]{DealFlow.Stage.WON, DealFlow.Stage.LOST}),
                "dealsWonThisMonth", deals.countByStageAndUpdatedAtBetween(DealFlow.Stage.WON, start, LocalDateTime.now()),
                "pendingTouchpoints", touchpoints.countByCompletedFalse()
        );
    }
}

