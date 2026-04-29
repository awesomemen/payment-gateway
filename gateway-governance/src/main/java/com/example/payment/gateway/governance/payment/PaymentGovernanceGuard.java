package com.example.payment.gateway.governance.payment;

public interface PaymentGovernanceGuard {

  void guardPaymentCreate(String merchantId);

  int currentPermitsPerMinute();
}
