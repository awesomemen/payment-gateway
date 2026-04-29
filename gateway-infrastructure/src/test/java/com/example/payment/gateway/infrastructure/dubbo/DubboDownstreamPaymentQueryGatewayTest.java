package com.example.payment.gateway.infrastructure.dubbo;

import com.example.payment.api.PaymentQueryFacade;
import com.example.payment.api.PaymentQueryFacadeRequest;
import com.example.payment.api.PaymentQueryFacadeResponse;
import com.example.payment.gateway.common.exception.GatewayException;
import com.example.payment.gateway.common.payment.DownstreamPaymentQueryRequest;
import com.example.payment.gateway.common.payment.DownstreamPaymentQueryResult;
import com.example.payment.gateway.common.payment.GatewayRouteDefinition;
import com.example.payment.gateway.common.response.GatewayResponseCodes;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class DubboDownstreamPaymentQueryGatewayTest {

  @Test
  void shouldMapSuccessfulQueryResponse() {
    PaymentQueryFacade paymentQueryFacade = Mockito.mock(PaymentQueryFacade.class);
    given(paymentQueryFacade.queryPayment(Mockito.any()))
        .willReturn(new PaymentQueryFacadeResponse("SUCCEEDED", "done"));

    DubboDownstreamPaymentQueryGateway gateway = new DubboDownstreamPaymentQueryGateway(paymentQueryFacade, mapper());

    DownstreamPaymentQueryResult result = gateway.query(route(), request());

    Assertions.assertEquals("SUCCEEDED", result.status());
    ArgumentCaptor<PaymentQueryFacadeRequest> captor = ArgumentCaptor.forClass(PaymentQueryFacadeRequest.class);
    verify(paymentQueryFacade).queryPayment(captor.capture());
    Assertions.assertEquals("REQ-QUERY-20260422-0001", captor.getValue().requestId());
  }

  @Test
  void shouldMapTimeoutLikeRpcExceptionToGatewayTimeout() {
    PaymentQueryFacade paymentQueryFacade = Mockito.mock(PaymentQueryFacade.class);
    given(paymentQueryFacade.queryPayment(Mockito.any()))
        .willThrow(new RpcException("payment query timed out in mock downstream facade"));

    DubboDownstreamPaymentQueryGateway gateway = new DubboDownstreamPaymentQueryGateway(paymentQueryFacade, mapper());

    assertThatThrownBy(() -> gateway.query(route(), request()))
        .isInstanceOf(GatewayException.class)
        .extracting(exception -> ((GatewayException) exception).code())
        .isEqualTo(GatewayResponseCodes.DOWNSTREAM_TIMEOUT);
  }

  @Test
  void shouldMapEmptyResponseToGatewayError() {
    PaymentQueryFacade paymentQueryFacade = Mockito.mock(PaymentQueryFacade.class);
    given(paymentQueryFacade.queryPayment(Mockito.any()))
        .willReturn(new PaymentQueryFacadeResponse(null, "empty"));

    DubboDownstreamPaymentQueryGateway gateway = new DubboDownstreamPaymentQueryGateway(paymentQueryFacade, mapper());

    assertThatThrownBy(() -> gateway.query(route(), request()))
        .isInstanceOf(GatewayException.class)
        .extracting(exception -> ((GatewayException) exception).code())
        .isEqualTo(GatewayResponseCodes.DOWNSTREAM_EMPTY_RESPONSE);
  }

  private static GatewayRouteDefinition route() {
    return new GatewayRouteDefinition(
        "ROUTE_PAY_QUERY",
        "PAY",
        "QUERY",
        "DUBBO",
        "com.example.payment.api.PaymentQueryFacade",
        "queryPayment",
        2000,
        0,
        "gateway:pay:query"
    );
  }

  private static DownstreamPaymentQueryRequest request() {
    return new DownstreamPaymentQueryRequest(
        "MCH100001",
        "REQ-QUERY-20260422-0001",
        "GP-QUERY-0001",
        "DSP-QUERY-0001",
        "PROCESSING",
        java.time.Instant.parse("2026-04-22T04:00:00Z")
    );
  }

  private static PaymentQueryFacadeMapper mapper() {
    return Mappers.getMapper(PaymentQueryFacadeMapper.class);
  }
}
