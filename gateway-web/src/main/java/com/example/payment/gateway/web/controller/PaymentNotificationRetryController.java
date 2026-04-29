package com.example.payment.gateway.web.controller;

import com.example.payment.gateway.api.payment.PaymentNotificationRetryResponse;
import com.example.payment.gateway.application.service.PaymentNotificationRetryApplicationService;
import com.example.payment.gateway.common.response.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/messaging/notifications")
public class PaymentNotificationRetryController {

  private final PaymentNotificationRetryApplicationService paymentNotificationRetryApplicationService;

  public PaymentNotificationRetryController(
      PaymentNotificationRetryApplicationService paymentNotificationRetryApplicationService
  ) {
    this.paymentNotificationRetryApplicationService = paymentNotificationRetryApplicationService;
  }

  @PostMapping("/retry")
  public ApiResponse<PaymentNotificationRetryResponse> retry() {
    return ApiResponse.success(paymentNotificationRetryApplicationService.retryFailedNotifications());
  }
}
