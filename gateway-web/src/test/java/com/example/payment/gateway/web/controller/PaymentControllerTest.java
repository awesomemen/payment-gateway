package com.example.payment.gateway.web.controller;

import com.example.payment.gateway.api.payment.PaymentCallbackResponse;
import com.example.payment.gateway.api.payment.PaymentCreateResponse;
import com.example.payment.gateway.api.payment.PaymentQueryResponse;
import com.example.payment.gateway.application.service.PaymentCallbackApplicationService;
import com.example.payment.gateway.application.service.PaymentCreateApplicationService;
import com.example.payment.gateway.application.service.PaymentQueryApplicationService;
import com.example.payment.gateway.common.exception.GatewayException;
import com.example.payment.gateway.common.response.GatewayResponseCodes;
import com.example.payment.gateway.web.advice.GatewayExceptionHandler;
import java.sql.SQLNonTransientConnectionException;
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
class PaymentControllerTest {

  private MockMvc mockMvc;

  @Mock
  private PaymentCreateApplicationService paymentCreateApplicationService;
  @Mock
  private PaymentQueryApplicationService paymentQueryApplicationService;
  @Mock
  private PaymentCallbackApplicationService paymentCallbackApplicationService;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
        .standaloneSetup(new PaymentController(
            paymentCreateApplicationService,
            paymentQueryApplicationService,
            paymentCallbackApplicationService
        ))
        .setControllerAdvice(new GatewayExceptionHandler())
        .build();
  }

  @Test
  void shouldReturnSuccessWhenPaymentIsAccepted() throws Exception {
    given(paymentCreateApplicationService.create(any()))
        .willReturn(new PaymentCreateResponse(
            "GP123456",
            "ACCEPTED",
            "ROUTE_PAY_CREATE",
            "Payment request accepted by gateway scaffold"
        ));

    mockMvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "merchantId": "MCH100001",
                  "requestId": "REQ-20260421-0001",
                  "idempotencyKey": "IDEMP-20260421-0001",
                  "amount": 88.50,
                  "currency": "CNY",
                  "requestTime": "2026-04-21T04:00:00Z",
                  "nonce": "nonce-001",
                  "signature": "demo-signature"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
        .andExpect(jsonPath("$.data.gatewayPaymentId").value("GP123456"));
  }

  @Test
  void shouldReturnDownstreamRejectedErrorWhenApplicationServiceRejectsRequest() throws Exception {
    given(paymentCreateApplicationService.create(any()))
        .willThrow(new GatewayException(
            GatewayResponseCodes.DOWNSTREAM_REJECTED,
            422,
            "payment rejected by downstream rule"
        ));

    mockMvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "merchantId": "MCH100001",
                  "requestId": "REQ-REJECT-20260422-0001",
                  "idempotencyKey": "IDEMP-REJECT-20260422-0001",
                  "amount": 88.50,
                  "currency": "CNY",
                  "requestTime": "2026-04-21T04:00:00Z",
                  "nonce": "nonce-001",
                  "signature": "demo-signature"
                }
                """))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value(GatewayResponseCodes.DOWNSTREAM_REJECTED));
  }

  @Test
  void shouldReturnDownstreamTimeoutErrorWhenApplicationServiceTimesOut() throws Exception {
    given(paymentCreateApplicationService.create(any()))
        .willThrow(new GatewayException(
            GatewayResponseCodes.DOWNSTREAM_TIMEOUT,
            504,
            "payment create timed out in mock downstream facade"
        ));

    mockMvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "merchantId": "MCH100001",
                  "requestId": "REQ-TIMEOUT-20260422-0001",
                  "idempotencyKey": "IDEMP-TIMEOUT-20260422-0001",
                  "amount": 88.50,
                  "currency": "CNY",
                  "requestTime": "2026-04-21T04:00:00Z",
                  "nonce": "nonce-001",
                  "signature": "demo-signature"
                }
                """))
        .andExpect(status().isGatewayTimeout())
        .andExpect(jsonPath("$.code").value(GatewayResponseCodes.DOWNSTREAM_TIMEOUT));
  }

  @Test
  void shouldReturnDownstreamServiceErrorWhenApplicationServiceInvocationFails() throws Exception {
    given(paymentCreateApplicationService.create(any()))
        .willThrow(new GatewayException(
            GatewayResponseCodes.DOWNSTREAM_SERVICE_ERROR,
            502,
            "payment create failed in mock downstream facade"
        ));

    mockMvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "merchantId": "MCH100001",
                  "requestId": "REQ-ERROR-20260422-0001",
                  "idempotencyKey": "IDEMP-ERROR-20260422-0001",
                  "amount": 88.50,
                  "currency": "CNY",
                  "requestTime": "2026-04-21T04:00:00Z",
                  "nonce": "nonce-001",
                  "signature": "demo-signature"
                }
                """))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.code").value(GatewayResponseCodes.DOWNSTREAM_SERVICE_ERROR));
  }

  @Test
  void shouldReturnRedisUnavailableWhenInfrastructureRedisFails() throws Exception {
    given(paymentCreateApplicationService.create(any()))
        .willThrow(new RuntimeException("Redis connection failure while reading idempotency state"));

    mockMvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "merchantId": "MCH100001",
                  "requestId": "REQ-REDIS-DOWN-20260429-0001",
                  "idempotencyKey": "IDEMP-REDIS-DOWN-20260429-0001",
                  "amount": 88.50,
                  "currency": "CNY",
                  "requestTime": "2026-04-29T04:00:00Z",
                  "nonce": "nonce-redis-001",
                  "signature": "demo-signature"
                }
                """))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value(GatewayResponseCodes.REDIS_UNAVAILABLE));
  }

  @Test
  void shouldReturnDatabaseUnavailableWhenInfrastructureDatabaseFails() throws Exception {
    given(paymentCreateApplicationService.create(any()))
        .willThrow(new RuntimeException("Unable to persist payment order",
            new SQLNonTransientConnectionException("MySQL connection is unavailable")));

    mockMvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "merchantId": "MCH100001",
                  "requestId": "REQ-MYSQL-DOWN-20260429-0001",
                  "idempotencyKey": "IDEMP-MYSQL-DOWN-20260429-0001",
                  "amount": 88.50,
                  "currency": "CNY",
                  "requestTime": "2026-04-29T04:00:00Z",
                  "nonce": "nonce-mysql-001",
                  "signature": "demo-signature"
                }
                """))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value(GatewayResponseCodes.DATABASE_UNAVAILABLE));
  }

  @Test
  void shouldReturnSuccessWhenPaymentQueryIsResolved() throws Exception {
    given(paymentQueryApplicationService.query(any()))
        .willReturn(new PaymentQueryResponse(
            "GP123456",
            "DSP123456",
            "SUCCEEDED",
            "ROUTE_PAY_QUERY",
            "Payment status loaded from downstream query"
        ));

    mockMvc.perform(post("/api/v1/payments/query")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "merchantId": "MCH100001",
                  "requestId": "REQ-QUERY-20260422-0001",
                  "gatewayPaymentId": "GP123456",
                  "requestTime": "2026-04-22T04:00:00Z",
                  "nonce": "nonce-query-001",
                  "signature": "demo-signature"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
        .andExpect(jsonPath("$.data.gatewayPaymentId").value("GP123456"));
  }

  @Test
  void shouldReturnSuccessWhenPaymentCallbackIsAccepted() throws Exception {
    given(paymentCallbackApplicationService.handle(any()))
        .willReturn(new PaymentCallbackResponse(
            "GP123456",
            "SUCCEEDED",
            "Payment callback accepted"
        ));

    mockMvc.perform(post("/api/v1/payments/callback")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "merchantId": "MCH100001",
                  "requestId": "REQ-CALLBACK-20260422-0001",
                  "gatewayPaymentId": "GP123456",
                  "downstreamPaymentId": "DSP123456",
                  "status": "SUCCEEDED",
                  "requestTime": "2026-04-22T04:00:00Z",
                  "nonce": "nonce-callback-001",
                  "signature": "demo-signature",
                  "message": "ok"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
  }
}
