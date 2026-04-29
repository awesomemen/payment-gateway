package com.example.payment.gateway.infrastructure.payment;

import com.example.payment.gateway.common.payment.DownstreamPaymentQueryGateway;
import com.example.payment.gateway.common.payment.DownstreamPaymentQueryRequest;
import com.example.payment.gateway.common.payment.DownstreamPaymentQueryResult;
import com.example.payment.gateway.common.payment.GatewayRouteDefinition;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("default")
public class InMemoryDownstreamPaymentQueryGateway implements DownstreamPaymentQueryGateway {

  @Override
  public DownstreamPaymentQueryResult query(GatewayRouteDefinition route, DownstreamPaymentQueryRequest request) {
    return new DownstreamPaymentQueryResult("SUCCEEDED", "payment succeeded in in-memory downstream facade");
  }
}
