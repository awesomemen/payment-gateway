package com.example.payment.gateway.security;

import com.example.payment.gateway.domain.model.SignedPaymentCommand;

public interface PaymentRequestSecurityValidator {

  default void validate(SignedPaymentCommand command) {
    validate(command, true);
  }

  void validate(SignedPaymentCommand command, boolean checkReplay);
}
