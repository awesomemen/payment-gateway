package com.example.payment.gateway.common.payment;

import java.util.Optional;

public interface GatewayRouteRepository {

  Optional<GatewayRouteDefinition> findRoute(String bizType, String apiCode);
}
