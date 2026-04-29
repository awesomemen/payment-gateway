package com.example.payment.gateway.api.system;

import java.util.List;

public record BootstrapStatusResponse(
    String project,
    String phase,
    String summary,
    List<String> activeProfiles,
    List<String> readyCapabilities,
    List<String> nextMilestones
) {
}
