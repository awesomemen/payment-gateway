package com.example.payment.gateway.common.payment;

public interface DownstreamPaymentQueryGateway {

  DownstreamPaymentQueryResult query(GatewayRouteDefinition route, DownstreamPaymentQueryRequest request);
}
