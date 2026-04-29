package com.example.payment.gateway.web.controller.payment;

import com.example.payment.gateway.api.payment.PaymentGovernanceConfigResponse;
import com.example.payment.gateway.application.service.PaymentGovernanceQueryApplicationService;
import com.example.payment.gateway.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/governance")
public class PaymentGovernanceController {

  private final PaymentGovernanceQueryApplicationService paymentGovernanceQueryApplicationService;

  public PaymentGovernanceController(PaymentGovernanceQueryApplicationService paymentGovernanceQueryApplicationService) {
    this.paymentGovernanceQueryApplicationService = paymentGovernanceQueryApplicationService;
  }

  @GetMapping("/config")
  public ApiResponse<PaymentGovernanceConfigResponse> currentConfig() {
    return ApiResponse.success(paymentGovernanceQueryApplicationService.currentConfig());
  }

  @PostMapping("/refresh")
  public ApiResponse<PaymentGovernanceConfigResponse> refresh() {
    return ApiResponse.success(paymentGovernanceQueryApplicationService.currentConfig());
  }
}
