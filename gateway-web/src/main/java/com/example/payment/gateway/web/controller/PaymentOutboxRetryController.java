package com.example.payment.gateway.web.controller;

import com.example.payment.gateway.api.payment.PaymentOutboxRetryResponse;
import com.example.payment.gateway.application.service.PaymentOutboxRetryApplicationService;
import com.example.payment.gateway.common.response.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"local", "docker"})
@RequestMapping("/api/v1/messaging/outbox")
public class PaymentOutboxRetryController {

  private final PaymentOutboxRetryApplicationService paymentOutboxRetryApplicationService;

  public PaymentOutboxRetryController(PaymentOutboxRetryApplicationService paymentOutboxRetryApplicationService) {
    this.paymentOutboxRetryApplicationService = paymentOutboxRetryApplicationService;
  }

  @PostMapping("/retry")
  public ApiResponse<PaymentOutboxRetryResponse> retryFailedMessages() {
    return ApiResponse.success(paymentOutboxRetryApplicationService.retryFailedMessages());
  }
}
