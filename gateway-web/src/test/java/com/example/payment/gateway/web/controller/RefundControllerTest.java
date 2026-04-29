package com.example.payment.gateway.web.controller;

import com.example.payment.gateway.api.payment.RefundCallbackResponse;
import com.example.payment.gateway.api.payment.RefundCreateResponse;
import com.example.payment.gateway.api.payment.RefundQueryResponse;
import com.example.payment.gateway.application.service.RefundCallbackApplicationService;
import com.example.payment.gateway.application.service.RefundCreateApplicationService;
import com.example.payment.gateway.application.service.RefundQueryApplicationService;
import com.example.payment.gateway.web.advice.GatewayExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RefundControllerTest {

  private MockMvc mockMvc;

  @Mock
  private RefundCreateApplicationService refundCreateApplicationService;
  @Mock
  private RefundQueryApplicationService refundQueryApplicationService;
  @Mock
  private RefundCallbackApplicationService refundCallbackApplicationService;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(
            new RefundController(refundCreateApplicationService, refundQueryApplicationService, refundCallbackApplicationService))
        .setControllerAdvice(new GatewayExceptionHandler())
        .build();
  }

  @Test
  void shouldCreateRefund() throws Exception {
    given(refundCreateApplicationService.create(any()))
        .willReturn(new RefundCreateResponse("GR123", "DSR123", "ACCEPTED", "ROUTE_REFUND_CREATE", "refund accepted"));

    mockMvc.perform(post("/api/v1/refunds")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "merchantId":"MCH100001",
                  "requestId":"REQ-REFUND-20260423-0001",
                  "gatewayPaymentId":"GP123",
                  "idempotencyKey":"RIDEMP-0001",
                  "amount":10.50,
                  "currency":"CNY",
                  "requestTime":"2026-04-23T01:00:00Z",
                  "nonce":"nonce-refund-001",
                  "signature":"demo-signature"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.gatewayRefundId").value("GR123"));
  }

  @Test
  void shouldQueryRefund() throws Exception {
    given(refundQueryApplicationService.query(any()))
        .willReturn(new RefundQueryResponse("GR123", "DSR123", "SUCCEEDED", "ROUTE_REFUND_QUERY", "refund succeeded"));

    mockMvc.perform(post("/api/v1/refunds/query")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "merchantId":"MCH100001",
                  "requestId":"REQ-REFUND-QUERY-20260423-0001",
                  "gatewayRefundId":"GR123",
                  "requestTime":"2026-04-23T01:00:00Z",
                  "nonce":"nonce-refund-query-001",
                  "signature":"demo-signature"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
  }

  @Test
  void shouldAcceptRefundCallback() throws Exception {
    given(refundCallbackApplicationService.handle(any()))
        .willReturn(new RefundCallbackResponse("GR123", "SUCCEEDED", "Refund callback accepted"));

    mockMvc.perform(post("/api/v1/refunds/callback")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "merchantId":"MCH100001",
                  "requestId":"REQ-REFUND-CALLBACK-20260423-0001",
                  "gatewayRefundId":"GR123",
                  "downstreamRefundId":"DSR123",
                  "status":"SUCCEEDED",
                  "requestTime":"2026-04-23T01:00:00Z",
                  "nonce":"nonce-refund-callback-001",
                  "signature":"demo-signature",
                  "message":"ok"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
  }
}
