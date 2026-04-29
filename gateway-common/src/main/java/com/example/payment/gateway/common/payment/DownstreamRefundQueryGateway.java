package com.example.payment.gateway.common.payment;

public interface DownstreamRefundQueryGateway {

  DownstreamRefundQueryResult query(GatewayRouteDefinition route, DownstreamRefundQueryRequest request);
}
