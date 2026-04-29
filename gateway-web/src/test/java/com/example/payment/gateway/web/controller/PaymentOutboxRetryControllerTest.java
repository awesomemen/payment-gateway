package com.example.payment.gateway.web.controller;

import com.example.payment.gateway.api.payment.PaymentOutboxRetryResponse;
import com.example.payment.gateway.application.service.PaymentOutboxRetryApplicationService;
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
class PaymentOutboxRetryControllerTest {

  private MockMvc mockMvc;

  @Mock
  private PaymentOutboxRetryApplicationService paymentOutboxRetryApplicationService;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
        .standaloneSetup(new PaymentOutboxRetryController(paymentOutboxRetryApplicationService))
        .setControllerAdvice(new GatewayExceptionHandler())
        .build();
  }

  @Test
  void shouldReturnRetrySummary() throws Exception {
    given(paymentOutboxRetryApplicationService.retryFailedMessages())
        .willReturn(new PaymentOutboxRetryResponse(2, 1, 1, List.of("MSG-RETRY-001")));

    mockMvc.perform(post("/api/v1/messaging/outbox/retry"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.data.scannedCount").value(2))
        .andExpect(jsonPath("$.data.succeededCount").value(1))
        .andExpect(jsonPath("$.data.failedCount").value(1))
        .andExpect(jsonPath("$.data.retriedMessageKeys[0]").value("MSG-RETRY-001"));
  }
}
