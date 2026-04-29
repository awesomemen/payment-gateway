package com.example.payment.gateway.infrastructure.payment;

import com.example.payment.gateway.common.payment.DownstreamPaymentCreateGateway;
import com.example.payment.gateway.common.payment.DownstreamPaymentCreateRequest;
import com.example.payment.gateway.common.payment.DownstreamPaymentCreateResult;
import com.example.payment.gateway.common.payment.GatewayRouteDefinition;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("default")
public class InMemoryDownstreamPaymentCreateGateway implements DownstreamPaymentCreateGateway {

  @Override
  public DownstreamPaymentCreateResult create(
      GatewayRouteDefinition route,
      String gatewayPaymentId,
      DownstreamPaymentCreateRequest request
  ) {
    return new DownstreamPaymentCreateResult(
        "DSP" + Integer.toUnsignedString((request.merchantId() + ":" + request.requestId()).hashCode()),
        "ACCEPTED",
        "Payment request accepted by in-memory downstream gateway"
    );
  }
}
