package com.example.payment.gateway.web.controller;

import com.example.payment.gateway.api.payment.PaymentNotificationRetryResponse;
import com.example.payment.gateway.application.service.PaymentNotificationRetryApplicationService;
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
class PaymentNotificationRetryControllerTest {

  private MockMvc mockMvc;

  @Mock
  private PaymentNotificationRetryApplicationService paymentNotificationRetryApplicationService;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
        .standaloneSetup(new PaymentNotificationRetryController(paymentNotificationRetryApplicationService))
        .setControllerAdvice(new GatewayExceptionHandler())
        .build();
  }

  @Test
  void shouldReturnRetrySummary() throws Exception {
    given(paymentNotificationRetryApplicationService.retryFailedNotifications())
        .willReturn(new PaymentNotificationRetryResponse(2, 1, 1, 1, List.of("MSG-1", "MSG-2")));

    mockMvc.perform(post("/api/v1/messaging/notifications/retry"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.data.scannedCount").value(2))
        .andExpect(jsonPath("$.data.deadLetteredCount").value(1));
  }
}
