package com.example.payment.gateway.infrastructure.dubbo;

import com.example.payment.api.RefundCreateFacadeRequest;
import com.example.payment.api.RefundCreateFacadeResponse;
import com.example.payment.api.RefundQueryFacadeRequest;
import com.example.payment.api.RefundQueryFacadeResponse;
import java.time.Instant;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockRefundFacadeImplTest {

  private final MockRefundFacadeImpl facade = new MockRefundFacadeImpl(new MockDownstreamRefundStore());

  @Test
  void shouldReturnProcessingStatusForProcessingRefundRequest() {
    RefundCreateFacadeResponse response = facade.createRefund(createRequest("REQ-REFUND-PROCESSING-20260423-0001"));

    assertThat(response.getStatus()).isEqualTo("PROCESSING");
    assertThat(response.getMessage()).isEqualTo("refund is processing in mock downstream facade");
  }

  @Test
  void shouldThrowTimeoutForRefundCreateTimeoutRequest() {
    assertThatThrownBy(() -> facade.createRefund(createRequest("REQ-REFUND-TIMEOUT-20260423-0001")))
        .isInstanceOf(RpcException.class)
        .hasMessageContaining("timed out");
  }

  @Test
  void shouldResolveAcceptedRefundFromSharedStoreDuringQuery() {
    RefundCreateFacadeResponse created = facade.createRefund(createRequest("REQ-REFUND-QUERY-20260423-0001"));

    RefundQueryFacadeResponse queried = facade.queryRefund(new RefundQueryFacadeRequest(
        "MCH100001",
        "REQ-REFUND-QUERY-CHECK-20260423-0001",
        "GR-20260423-0001",
        created.getDownstreamRefundId(),
        created.getStatus(),
        Instant.parse("2026-04-23T04:01:00Z"),
        "ROUTE_REFUND_QUERY"
    ));

    assertThat(queried.getStatus()).isEqualTo("SUCCEEDED");
    assertThat(queried.getMessage()).isEqualTo("refund succeeded in mock downstream facade");
  }

  @Test
  void shouldKeepProcessingRefundWhenCreatedRefundIsStillProcessing() {
    RefundCreateFacadeResponse created = facade.createRefund(createRequest("REQ-REFUND-PROCESSING-20260423-0002"));

    RefundQueryFacadeResponse queried = facade.queryRefund(new RefundQueryFacadeRequest(
        "MCH100001",
        "REQ-REFUND-QUERY-CHECK-20260423-0002",
        "GR-20260423-0001",
        created.getDownstreamRefundId(),
        created.getStatus(),
        Instant.parse("2026-04-23T04:02:00Z"),
        "ROUTE_REFUND_QUERY"
    ));

    assertThat(queried.getStatus()).isEqualTo("PROCESSING");
    assertThat(queried.getMessage()).isEqualTo("refund is still processing in mock downstream facade");
  }

  private static RefundCreateFacadeRequest createRequest(String requestId) {
    return new RefundCreateFacadeRequest(
        "MCH100001",
        "GR-20260423-0001",
        "GP-20260423-0001",
        requestId,
        "IDEMP-REFUND-20260423-0001",
        "18.5",
        "CNY",
        Instant.parse("2026-04-23T04:00:00Z"),
        "ROUTE_REFUND_CREATE"
    );
  }
}
