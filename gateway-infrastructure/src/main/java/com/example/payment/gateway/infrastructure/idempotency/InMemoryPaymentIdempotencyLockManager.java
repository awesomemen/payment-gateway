package com.example.payment.gateway.infrastructure.idempotency;

import com.example.payment.gateway.common.idempotency.PaymentIdempotencyLock;
import com.example.payment.gateway.common.idempotency.PaymentIdempotencyLockManager;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("default")
public class InMemoryPaymentIdempotencyLockManager implements PaymentIdempotencyLockManager {

  private final Set<String> acquiredLocks = ConcurrentHashMap.newKeySet();

  @Override
  public Optional<PaymentIdempotencyLock> tryLock(
      String merchantId,
      String idempotencyKey,
      Duration waitDuration,
      Duration leaseDuration
  ) {
    String key = lockKey(merchantId, idempotencyKey);
    boolean acquired = acquiredLocks.add(key);
    if (!acquired) {
      return Optional.empty();
    }
    return Optional.of(() -> acquiredLocks.remove(key));
  }

  private static String lockKey(String merchantId, String idempotencyKey) {
    return merchantId + ":" + idempotencyKey;
  }
}
