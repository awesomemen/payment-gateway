package com.example.payment.gateway.web.controller.payment;

import com.example.payment.gateway.api.payment.PaymentAuditSummaryResponse;
import com.example.payment.gateway.application.service.PaymentAuditQueryApplicationService;
import com.example.payment.gateway.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
public class PaymentAuditController {

  private final PaymentAuditQueryApplicationService paymentAuditQueryApplicationService;

  public PaymentAuditController(PaymentAuditQueryApplicationService paymentAuditQueryApplicationService) {
    this.paymentAuditQueryApplicationService = paymentAuditQueryApplicationService;
  }

  @GetMapping("/summary")
  public ApiResponse<PaymentAuditSummaryResponse> summary() {
    return ApiResponse.success(paymentAuditQueryApplicationService.currentSummary());
  }
}
