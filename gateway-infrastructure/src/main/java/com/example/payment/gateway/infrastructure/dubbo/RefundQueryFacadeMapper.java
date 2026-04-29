package com.example.payment.gateway.infrastructure.dubbo;

import com.example.payment.api.RefundQueryFacadeRequest;
import com.example.payment.gateway.common.payment.DownstreamRefundQueryRequest;
import com.example.payment.gateway.common.payment.GatewayRouteDefinition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RefundQueryFacadeMapper {

  @Mapping(target = "merchantId", source = "request.merchantId")
  @Mapping(target = "requestId", source = "request.requestId")
  @Mapping(target = "gatewayRefundId", source = "request.gatewayRefundId")
  @Mapping(target = "downstreamRefundId", source = "request.downstreamRefundId")
  @Mapping(target = "currentStatus", source = "request.currentStatus")
  @Mapping(target = "requestTime", source = "request.requestTime")
  @Mapping(target = "routeCode", source = "route.routeCode")
  RefundQueryFacadeRequest toFacadeRequest(GatewayRouteDefinition route, DownstreamRefundQueryRequest request);
}
