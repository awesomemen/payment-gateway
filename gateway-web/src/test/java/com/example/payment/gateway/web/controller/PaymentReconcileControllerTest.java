package com.example.payment.gateway.web.controller;

import com.example.payment.gateway.api.payment.PaymentReconcileResponse;
import com.example.payment.gateway.application.service.PaymentReconcileApplicationService;
import com.example.payment.gateway.web.advice.GatewayExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentReconcileControllerTest {

  private MockMvc mockMvc;

  @Mock
  private PaymentReconcileApplicationService paymentReconcileApplicationService;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
        .standaloneSetup(new PaymentReconcileController(paymentReconcileApplicationService))
        .setControllerAdvice(new GatewayExceptionHandler())
        .build();
  }

  @Test
  void shouldReturnReconcileSummary() throws Exception {
    given(paymentReconcileApplicationService.reconcileProcessingOrders())
        .willReturn(new PaymentReconcileResponse(2, 1, 1, 0, List.of("GP-RECON-001")));

    mockMvc.perform(post("/api/v1/payments/reconcile/processing"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.data.scannedCount").value(2))
        .andExpect(jsonPath("$.data.updatedCount").value(1))
        .andExpect(jsonPath("$.data.updatedGatewayPaymentIds[0]").value("GP-RECON-001"));
  }
}
