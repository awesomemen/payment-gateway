package com.example.payment.gateway.api.tech;

import java.util.List;

public record TechScaffoldStatusResponse(
    String project,
    String stage,
    List<TechComponentStatusResponse> components
) {
}
