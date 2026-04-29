package com.example.payment.gateway.common.idempotency;

import java.time.Duration;
import java.util.Optional;

public interface PaymentIdempotencyLockManager {

  Optional<PaymentIdempotencyLock> tryLock(
      String merchantId,
      String idempotencyKey,
      Duration waitDuration,
      Duration leaseDuration
  );
}
