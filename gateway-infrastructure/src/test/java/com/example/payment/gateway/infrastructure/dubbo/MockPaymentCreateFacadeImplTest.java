package com.example.payment.gateway.infrastructure.dubbo;

import com.example.payment.api.PaymentCreateFacadeRequest;
import com.example.payment.api.PaymentCreateFacadeResponse;
import com.example.payment.api.PaymentQueryFacadeRequest;
import com.example.payment.api.PaymentQueryFacadeResponse;
import java.time.Instant;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockPaymentCreateFacadeImplTest {

  private final MockDownstreamPaymentStore paymentStore = new MockDownstreamPaymentStore();
  private final MockPaymentCreateFacadeImpl facade = new MockPaymentCreateFacadeImpl(paymentStore);

  @Test
  void shouldReturnRejectedStatusForRejectRequestId() {
    PaymentCreateFacadeResponse response = facade.createPayment(request("REQ-REJECT-20260422-0001"));

    assertThat(response.status()).isEqualTo("REJECTED");
    assertThat(response.message()).isEqualTo("payment rejected by mock downstream facade");
  }

  @Test
  void shouldReturnFailedStatusForFailRequestId() {
    PaymentCreateFacadeResponse response = facade.createPayment(request("REQ-FAIL-20260422-0001"));

    assertThat(response.status()).isEqualTo("FAILED");
    assertThat(response.message()).isEqualTo("payment failed in mock downstream facade");
  }

  @Test
  void shouldReturnProcessingStatusForProcessingRequestId() {
    PaymentCreateFacadeResponse response = facade.createPayment(request("REQ-PROCESSING-20260422-0001"));

    assertThat(response.status()).isEqualTo("PROCESSING");
    assertThat(response.message()).isEqualTo("payment is processing in mock downstream facade");
  }

  @Test
  void shouldThrowTimeoutRpcExceptionForTimeoutRequestId() {
    assertThatThrownBy(() -> facade.createPayment(request("REQ-TIMEOUT-20260422-0001")))
        .isInstanceOf(RpcException.class)
        .extracting("message")
        .isEqualTo("payment create timed out in mock downstream facade");
  }

  @Test
  void shouldThrowRpcExceptionForErrorRequestId() {
    assertThatThrownBy(() -> facade.createPayment(request("REQ-ERROR-20260422-0001")))
        .isInstanceOf(RpcException.class)
        .extracting("message")
        .isEqualTo("payment create failed in mock downstream facade");
  }

  @Test
  void shouldPersistAcceptedPaymentForSharedContractQuery() {
    PaymentCreateFacadeResponse created = facade.createPayment(request("REQ-QUERY-20260422-0001"));
    MockPaymentQueryFacadeImpl queryFacade = new MockPaymentQueryFacadeImpl(paymentStore);

    PaymentQueryFacadeResponse queried = queryFacade.queryPayment(new PaymentQueryFacadeRequest(
        "MCH100001",
        "REQ-QUERY-20260422-0001",
        "GP-20260422-0001",
        created.downstreamPaymentId(),
        created.status(),
        Instant.parse("2026-04-22T10:01:00Z")
    ));

    assertThat(queried.status()).isEqualTo("SUCCEEDED");
    assertThat(queried.message()).isEqualTo("payment succeeded in mock downstream facade");
  }

  private static PaymentCreateFacadeRequest request(String requestId) {
    return new PaymentCreateFacadeRequest(
        "MCH100001",
        "GP-20260422-0001",
        requestId,
        "IDEMP-20260422-0001",
        "88.5",
        "CNY",
        Instant.parse("2026-04-22T10:00:00Z"),
        "ROUTE_PAY_CREATE"
    );
  }
}
