package com.example.payment.gateway.infrastructure.payment;

import com.example.payment.gateway.common.payment.DownstreamRefundCreateGateway;
import com.example.payment.gateway.common.payment.DownstreamRefundCreateRequest;
import com.example.payment.gateway.common.payment.DownstreamRefundCreateResult;
import com.example.payment.gateway.common.payment.GatewayRouteDefinition;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("default")
public class InMemoryDownstreamRefundCreateGateway implements DownstreamRefundCreateGateway {

  @Override
  public DownstreamRefundCreateResult create(
      GatewayRouteDefinition route,
      String gatewayRefundId,
      DownstreamRefundCreateRequest request
  ) {
    return new DownstreamRefundCreateResult(
        "DSR" + Integer.toUnsignedString((request.merchantId() + ":" + request.requestId()).hashCode()),
        "ACCEPTED",
        "Refund accepted by in-memory downstream gateway"
    );
  }
}
