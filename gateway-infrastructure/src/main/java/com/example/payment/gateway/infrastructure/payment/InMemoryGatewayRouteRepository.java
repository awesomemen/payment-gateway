package com.example.payment.gateway.infrastructure.payment;

import com.example.payment.gateway.common.payment.GatewayRouteDefinition;
import com.example.payment.gateway.common.payment.GatewayRouteRepository;
import com.example.payment.gateway.common.payment.PaymentBizTypes;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("default")
public class InMemoryGatewayRouteRepository implements GatewayRouteRepository {

  @Override
  public Optional<GatewayRouteDefinition> findRoute(String bizType, String apiCode) {
    if (!PaymentBizTypes.PAY.equals(bizType) || !PaymentBizTypes.CREATE.equals(apiCode)) {
      return Optional.empty();
    }
    return Optional.of(new GatewayRouteDefinition(
        "ROUTE_PAY_CREATE",
        PaymentBizTypes.PAY,
        PaymentBizTypes.CREATE,
        "DUBBO",
        "com.example.payment.api.PaymentCreateFacade",
        "createPayment",
        3000,
        0,
        "gateway:pay:create"
    ));
  }
}
