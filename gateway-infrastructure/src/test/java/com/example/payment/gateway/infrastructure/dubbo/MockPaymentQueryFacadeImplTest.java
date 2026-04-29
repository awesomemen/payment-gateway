package com.example.payment.gateway.infrastructure.dubbo;

import com.example.payment.api.PaymentQueryFacadeRequest;
import com.example.payment.api.PaymentQueryFacadeResponse;
import java.time.Instant;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockPaymentQueryFacadeImplTest {

  private final MockDownstreamPaymentStore paymentStore = new MockDownstreamPaymentStore();
  private final MockPaymentCreateFacadeImpl createFacade = new MockPaymentCreateFacadeImpl(paymentStore);
  private final MockPaymentQueryFacadeImpl facade = new MockPaymentQueryFacadeImpl(paymentStore);

  @Test
  void shouldReturnProcessingForProcessingRequestId() {
    PaymentQueryFacadeResponse response = facade.queryPayment(request("REQ-PROCESSING-20260422-0001"));

    Assertions.assertEquals("PROCESSING", response.status());
  }

  @Test
  void shouldReturnSucceededForRegularRequestId() {
    PaymentQueryFacadeResponse response = facade.queryPayment(request("REQ-QUERY-20260422-0001"));

    Assertions.assertEquals("SUCCEEDED", response.status());
  }

  @Test
  void shouldThrowRpcExceptionForTimeoutRequestId() {
    assertThatThrownBy(() -> facade.queryPayment(request("REQ-TIMEOUT-20260422-0001")))
        .isInstanceOf(RpcException.class)
        .hasMessageContaining("timed out");
  }

  @Test
  void shouldReturnStoredProcessingStatusForCreatedPayment() {
    createFacade.createPayment(paymentCreateRequest("REQ-PROCESSING-20260422-0001"));

    PaymentQueryFacadeResponse response = facade.queryPayment(request("REQ-PROCESSING-20260422-0001"));

    Assertions.assertEquals("PROCESSING", response.status());
    Assertions.assertEquals("payment is still processing in mock downstream facade", response.message());
  }

  private static com.example.payment.api.PaymentCreateFacadeRequest paymentCreateRequest(String requestId) {
    return new com.example.payment.api.PaymentCreateFacadeRequest(
        "MCH100001",
        "GP-QUERY-0001",
        requestId,
        "IDEMP-20260422-0001",
        "88.5",
        "CNY",
        Instant.parse("2026-04-22T04:00:00Z"),
        "ROUTE_PAY_CREATE"
    );
  }

  private static PaymentQueryFacadeRequest request(String requestId) {
    return new PaymentQueryFacadeRequest(
        "MCH100001",
        requestId,
        "GP-QUERY-0001",
        "DSP-QUERY-0001",
        "PROCESSING",
        java.time.Instant.parse("2026-04-22T04:00:00Z")
    );
  }
}
