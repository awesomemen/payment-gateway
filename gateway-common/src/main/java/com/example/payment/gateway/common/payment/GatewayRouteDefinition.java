package com.example.payment.gateway.common.payment;

public record GatewayRouteDefinition(
    String routeCode,
    String bizType,
    String apiCode,
    String targetProtocol,
    String targetService,
    String targetMethod,
    int timeoutMillis,
    int retryTimes,
    String sentinelResource
) {
}
