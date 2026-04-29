package com.example.payment.gateway.infrastructure.payment;

import com.example.payment.gateway.common.payment.DownstreamRefundQueryGateway;
import com.example.payment.gateway.common.payment.DownstreamRefundQueryRequest;
import com.example.payment.gateway.common.payment.DownstreamRefundQueryResult;
import com.example.payment.gateway.common.payment.GatewayRouteDefinition;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("default")
public class InMemoryDownstreamRefundQueryGateway implements DownstreamRefundQueryGateway {

  @Override
  public DownstreamRefundQueryResult query(GatewayRouteDefinition route, DownstreamRefundQueryRequest request) {
    return new DownstreamRefundQueryResult("SUCCEEDED", "refund succeeded in in-memory downstream facade");
  }
}
