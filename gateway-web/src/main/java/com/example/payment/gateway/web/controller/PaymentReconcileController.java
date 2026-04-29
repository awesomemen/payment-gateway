package com.example.payment.gateway.web.controller;

import com.example.payment.gateway.api.payment.PaymentReconcileResponse;
import com.example.payment.gateway.application.service.PaymentReconcileApplicationService;
import com.example.payment.gateway.common.response.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/reconcile")
public class PaymentReconcileController {

  private final PaymentReconcileApplicationService paymentReconcileApplicationService;

  public PaymentReconcileController(PaymentReconcileApplicationService paymentReconcileApplicationService) {
    this.paymentReconcileApplicationService = paymentReconcileApplicationService;
  }

  @PostMapping("/processing")
  public ApiResponse<PaymentReconcileResponse> reconcileProcessingOrders() {
    return ApiResponse.success(paymentReconcileApplicationService.reconcileProcessingOrders());
  }
}
