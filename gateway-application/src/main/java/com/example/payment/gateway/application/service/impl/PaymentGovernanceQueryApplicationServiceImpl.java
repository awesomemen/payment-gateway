package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.PaymentGovernanceConfigResponse;
import com.example.payment.gateway.application.service.PaymentGovernanceQueryApplicationService;
import com.example.payment.gateway.governance.payment.PaymentGovernanceGuard;
import org.springframework.stereotype.Service;

@Service
public class PaymentGovernanceQueryApplicationServiceImpl implements PaymentGovernanceQueryApplicationService {

  private final PaymentGovernanceGuard paymentGovernanceGuard;

  public PaymentGovernanceQueryApplicationServiceImpl(PaymentGovernanceGuard paymentGovernanceGuard) {
    this.paymentGovernanceGuard = paymentGovernanceGuard;
  }

  @Override
  public PaymentGovernanceConfigResponse currentConfig() {
    return new PaymentGovernanceConfigResponse(paymentGovernanceGuard.currentPermitsPerMinute());
  }
}
