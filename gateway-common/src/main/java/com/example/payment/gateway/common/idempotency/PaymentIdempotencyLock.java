package com.example.payment.gateway.common.idempotency;

public interface PaymentIdempotencyLock {

  void release();
}
