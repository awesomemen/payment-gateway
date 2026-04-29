package com.example.payment.gateway.infrastructure.dubbo;

import com.example.payment.api.PaymentCreateFacade;
import com.example.payment.api.PaymentCreateFacadeRequest;
import com.example.payment.api.PaymentCreateFacadeResponse;
import com.example.payment.gateway.common.exception.GatewayException;
import com.example.payment.gateway.common.payment.DownstreamPaymentCreateRequest;
import com.example.payment.gateway.common.payment.DownstreamPaymentCreateResult;
import com.example.payment.gateway.common.payment.GatewayRouteDefinition;
import com.example.payment.gateway.common.response.GatewayResponseCodes;
import java.time.Instant;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mockito;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class DubboDownstreamPaymentCreateGatewayTest {

  @Test
  void shouldMapFacadeResponseToDownstreamResult() {
    PaymentCreateFacade paymentCreateFacade = Mockito.mock(PaymentCreateFacade.class);
    given(paymentCreateFacade.createPayment(any()))
        .willReturn(new PaymentCreateFacadeResponse(
            "DSP-20260422-0001",
            "ACCEPTED",
            "accepted by downstream facade"
        ));
    DubboDownstreamPaymentCreateGateway gateway = new DubboDownstreamPaymentCreateGateway(paymentCreateFacade, mapper());

    DownstreamPaymentCreateResult result = gateway.create(route(), "GP-20260422-0001", request());

    assertThat(result.downstreamPaymentId()).isEqualTo("DSP-20260422-0001");
    assertThat(result.status()).isEqualTo("ACCEPTED");
    assertThat(result.message()).isEqualTo("accepted by downstream facade");
  }

  @Test
  void shouldRejectEmptyFacadeResponse() {
    PaymentCreateFacade paymentCreateFacade = Mockito.mock(PaymentCreateFacade.class);
    given(paymentCreateFacade.createPayment(any())).willReturn(null);
    DubboDownstreamPaymentCreateGateway gateway = new DubboDownstreamPaymentCreateGateway(paymentCreateFacade, mapper());

    assertThatThrownBy(() -> gateway.create(route(), "GP-20260422-0001", request()))
        .isInstanceOf(GatewayException.class)
        .extracting("code")
        .isEqualTo(GatewayResponseCodes.DOWNSTREAM_EMPTY_RESPONSE);
  }

  @Test
  void shouldMapRejectedFacadeStatusToDistinctGatewayError() {
    PaymentCreateFacade paymentCreateFacade = Mockito.mock(PaymentCreateFacade.class);
    given(paymentCreateFacade.createPayment(any()))
        .willReturn(new PaymentCreateFacadeResponse(
            "DSP-20260422-0002",
            "REJECTED",
            "payment rejected by downstream rule"
        ));
    DubboDownstreamPaymentCreateGateway gateway = new DubboDownstreamPaymentCreateGateway(paymentCreateFacade, mapper());

    assertThatThrownBy(() -> gateway.create(route(), "GP-20260422-0002", request()))
        .isInstanceOf(GatewayException.class)
        .extracting("code")
        .isEqualTo(GatewayResponseCodes.DOWNSTREAM_REJECTED);
  }

  @Test
  void shouldMapFailedFacadeStatusToDistinctGatewayError() {
    PaymentCreateFacade paymentCreateFacade = Mockito.mock(PaymentCreateFacade.class);
    given(paymentCreateFacade.createPayment(any()))
        .willReturn(new PaymentCreateFacadeResponse(
            "DSP-20260422-0003",
            "FAILED",
            "payment failed in downstream system"
        ));
    DubboDownstreamPaymentCreateGateway gateway = new DubboDownstreamPaymentCreateGateway(paymentCreateFacade, mapper());

    assertThatThrownBy(() -> gateway.create(route(), "GP-20260422-0003", request()))
        .isInstanceOf(GatewayException.class)
        .extracting("code")
        .isEqualTo(GatewayResponseCodes.DOWNSTREAM_FAILED);
  }

  @Test
  void shouldMapProcessingFacadeStatusToSuccessfulProcessingResult() {
    PaymentCreateFacade paymentCreateFacade = Mockito.mock(PaymentCreateFacade.class);
    given(paymentCreateFacade.createPayment(any()))
        .willReturn(new PaymentCreateFacadeResponse(
            "DSP-20260422-0004",
            "PROCESSING",
            "payment is processing in downstream facade"
        ));
    DubboDownstreamPaymentCreateGateway gateway = new DubboDownstreamPaymentCreateGateway(
        paymentCreateFacade,
        mapper()
    );

    DownstreamPaymentCreateResult result = gateway.create(route(), "GP-20260422-0004", request());

    assertThat(result.downstreamPaymentId()).isEqualTo("DSP-20260422-0004");
    assertThat(result.status()).isEqualTo("PROCESSING");
    assertThat(result.message()).isEqualTo("payment is processing in downstream facade");
  }

  @Test
  void shouldMapTimeoutRpcExceptionToDistinctGatewayError() {
    PaymentCreateFacade paymentCreateFacade = Mockito.mock(PaymentCreateFacade.class);
    given(paymentCreateFacade.createPayment(any()))
        .willThrow(new RpcException(
            RpcException.TIMEOUT_EXCEPTION,
            "payment create timed out in mock downstream facade"
        ));
    DubboDownstreamPaymentCreateGateway gateway = new DubboDownstreamPaymentCreateGateway(
        paymentCreateFacade,
        mapper()
    );

    assertThatThrownBy(() -> gateway.create(route(), "GP-20260422-0006", request()))
        .isInstanceOf(GatewayException.class)
        .extracting("code")
        .isEqualTo(GatewayResponseCodes.DOWNSTREAM_TIMEOUT);
  }

  @Test
  void shouldMapRpcExceptionToDownstreamServiceError() {
    PaymentCreateFacade paymentCreateFacade = Mockito.mock(PaymentCreateFacade.class);
    given(paymentCreateFacade.createPayment(any()))
        .willThrow(new RpcException("payment create failed in mock downstream facade"));
    DubboDownstreamPaymentCreateGateway gateway = new DubboDownstreamPaymentCreateGateway(
        paymentCreateFacade,
        mapper()
    );

    assertThatThrownBy(() -> gateway.create(route(), "GP-20260422-0007", request()))
        .isInstanceOf(GatewayException.class)
        .extracting("code")
        .isEqualTo(GatewayResponseCodes.DOWNSTREAM_SERVICE_ERROR);
  }

  @Test
  void shouldMapTimeoutLikeRpcExceptionMessageToTimeoutGatewayError() {
    PaymentCreateFacade paymentCreateFacade = Mockito.mock(PaymentCreateFacade.class);
    given(paymentCreateFacade.createPayment(any()))
        .willThrow(new RpcException("payment create timed out in mock downstream facade"));
    DubboDownstreamPaymentCreateGateway gateway = new DubboDownstreamPaymentCreateGateway(
        paymentCreateFacade,
        mapper()
    );

    assertThatThrownBy(() -> gateway.create(route(), "GP-20260422-0008", request()))
        .isInstanceOf(GatewayException.class)
        .extracting("code")
        .isEqualTo(GatewayResponseCodes.DOWNSTREAM_TIMEOUT);
  }

  @Test
  void shouldMapRouteAndGatewayFieldsIntoFacadeRequest() {
    PaymentCreateFacade paymentCreateFacade = Mockito.mock(PaymentCreateFacade.class);
    given(paymentCreateFacade.createPayment(any()))
        .willReturn(new PaymentCreateFacadeResponse(
            "DSP-20260422-0005",
            "ACCEPTED",
            "accepted by downstream facade"
        ));
    DubboDownstreamPaymentCreateGateway gateway = new DubboDownstreamPaymentCreateGateway(
        paymentCreateFacade,
        mapper()
    );

    gateway.create(route(), "GP-20260422-0005", request());

    ArgumentCaptor<PaymentCreateFacadeRequest> captor = ArgumentCaptor.forClass(PaymentCreateFacadeRequest.class);
    verify(paymentCreateFacade).createPayment(captor.capture());
    assertThat(captor.getValue().merchantId()).isEqualTo("MCH100001");
    assertThat(captor.getValue().gatewayPaymentId()).isEqualTo("GP-20260422-0005");
    assertThat(captor.getValue().requestId()).isEqualTo("REQ-20260422-0001");
    assertThat(captor.getValue().idempotencyKey()).isEqualTo("IDEMP-20260422-0001");
    assertThat(captor.getValue().amount()).isEqualTo("88.5");
    assertThat(captor.getValue().currency()).isEqualTo("CNY");
    assertThat(captor.getValue().requestTime()).isEqualTo(Instant.parse("2026-04-22T10:00:00Z"));
    assertThat(captor.getValue().routeCode()).isEqualTo("ROUTE_PAY_CREATE");
  }

  private static GatewayRouteDefinition route() {
    return new GatewayRouteDefinition(
        "ROUTE_PAY_CREATE",
        "PAY",
        "CREATE",
        "DUBBO",
        "com.example.payment.api.PaymentCreateFacade",
        "createPayment",
        3000,
        0,
        "gateway:pay:create"
    );
  }

  private static DownstreamPaymentCreateRequest request() {
    return new DownstreamPaymentCreateRequest(
        "MCH100001",
        "REQ-20260422-0001",
        "IDEMP-20260422-0001",
        "88.5",
        "CNY",
        Instant.parse("2026-04-22T10:00:00Z")
    );
  }

  private static PaymentCreateFacadeMapper mapper() {
    return Mappers.getMapper(PaymentCreateFacadeMapper.class);
  }
}
