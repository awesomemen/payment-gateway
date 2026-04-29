package com.example.payment.gateway.common.payment;

public interface DownstreamPaymentCreateGateway {

  DownstreamPaymentCreateResult create(
      GatewayRouteDefinition route,
      String gatewayPaymentId,
      DownstreamPaymentCreateRequest request
  );
}
