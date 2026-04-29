package com.example.payment.gateway.domain.model;

import java.time.Instant;

public interface SignedPaymentCommand {

  String merchantId();

  String requestId();

  Instant requestTime();

  String nonce();

  String signature();

  String signaturePayload();
}
