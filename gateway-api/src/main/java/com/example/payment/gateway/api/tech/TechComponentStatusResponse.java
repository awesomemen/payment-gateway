package com.example.payment.gateway.api.tech;

public record TechComponentStatusResponse(
    String component,
    String status,
    String detail
) {
}
