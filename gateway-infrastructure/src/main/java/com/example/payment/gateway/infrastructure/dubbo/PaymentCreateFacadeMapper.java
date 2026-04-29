package com.example.payment.gateway.infrastructure.dubbo;

import com.example.payment.api.PaymentCreateFacadeRequest;
import com.example.payment.gateway.common.payment.DownstreamPaymentCreateRequest;
import com.example.payment.gateway.common.payment.GatewayRouteDefinition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentCreateFacadeMapper {

  @Mapping(target = "merchantId", source = "request.merchantId")
  @Mapping(target = "gatewayPaymentId", source = "gatewayPaymentId")
  @Mapping(target = "requestId", source = "request.requestId")
  @Mapping(target = "idempotencyKey", source = "request.idempotencyKey")
  @Mapping(target = "amount", source = "request.amount")
  @Mapping(target = "currency", source = "request.currency")
  @Mapping(target = "requestTime", source = "request.requestTime")
  @Mapping(target = "routeCode", source = "route.routeCode")
  PaymentCreateFacadeRequest toFacadeRequest(
      GatewayRouteDefinition route,
      String gatewayPaymentId,
      DownstreamPaymentCreateRequest request
  );
}
