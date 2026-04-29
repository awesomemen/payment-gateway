package com.example.payment.gateway.common.payment;

public interface DownstreamRefundCreateGateway {

  DownstreamRefundCreateResult create(GatewayRouteDefinition route, String gatewayRefundId, DownstreamRefundCreateRequest request);
}
