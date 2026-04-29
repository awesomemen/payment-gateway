package com.example.payment.gateway.infrastructure.dubbo;

import com.example.payment.api.PaymentQueryFacadeRequest;
import com.example.payment.gateway.common.payment.DownstreamPaymentQueryRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentQueryFacadeMapper {

  PaymentQueryFacadeRequest toFacadeRequest(DownstreamPaymentQueryRequest request);
}
