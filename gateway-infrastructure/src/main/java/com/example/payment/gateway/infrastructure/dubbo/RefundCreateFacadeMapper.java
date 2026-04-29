package com.example.payment.gateway.infrastructure.dubbo;

import com.example.payment.api.RefundCreateFacadeRequest;
import com.example.payment.gateway.common.payment.DownstreamRefundCreateRequest;
import com.example.payment.gateway.common.payment.GatewayRouteDefinition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RefundCreateFacadeMapper {

  @Mapping(target = "merchantId", source = "request.merchantId")
  @Mapping(target = "gatewayRefundId", source = "gatewayRefundId")
  @Mapping(target = "gatewayPaymentId", source = "request.gatewayPaymentId")
  @Mapping(target = "requestId", source = "request.requestId")
  @Mapping(target = "idempotencyKey", source = "request.idempotencyKey")
  @Mapping(target = "amount", source = "request.amount")
  @Mapping(target = "currency", source = "request.currency")
  @Mapping(target = "requestTime", source = "request.requestTime")
  @Mapping(target = "routeCode", source = "route.routeCode")
  RefundCreateFacadeRequest toFacadeRequest(
      GatewayRouteDefinition route,
      String gatewayRefundId,
      DownstreamRefundCreateRequest request
  );
}
