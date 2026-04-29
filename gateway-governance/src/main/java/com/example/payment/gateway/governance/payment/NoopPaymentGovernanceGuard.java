package com.example.payment.gateway.governance.payment;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("default")
public class NoopPaymentGovernanceGuard implements PaymentGovernanceGuard {

  @Override
  public void guardPaymentCreate(String merchantId) {
  }

  @Override
  public int currentPermitsPerMinute() {
    return Integer.MAX_VALUE;
  }
}
