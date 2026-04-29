package com.example.payment.gateway.infrastructure.idempotency;

import com.example.payment.gateway.common.idempotency.PaymentIdempotencyLock;
import com.example.payment.gateway.common.idempotency.PaymentIdempotencyLockManager;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "docker"})
public class RedissonPaymentIdempotencyLockManager implements PaymentIdempotencyLockManager {

  private final RedissonClient redissonClient;

  public RedissonPaymentIdempotencyLockManager(RedissonClient redissonClient) {
    this.redissonClient = redissonClient;
  }

  @Override
  public Optional<PaymentIdempotencyLock> tryLock(
      String merchantId,
      String idempotencyKey,
      Duration waitDuration,
      Duration leaseDuration
  ) {
    RLock lock = redissonClient.getLock(lockKey(merchantId, idempotencyKey));
    try {
      boolean acquired = lock.tryLock(
          waitDuration.toMillis(),
          leaseDuration.toMillis(),
          TimeUnit.MILLISECONDS
      );
      if (!acquired) {
        return Optional.empty();
      }
      return Optional.of(() -> {
        if (lock.isHeldByCurrentThread()) {
          lock.unlock();
        }
      });
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while acquiring idempotency lock", exception);
    }
  }

  private static String lockKey(String merchantId, String idempotencyKey) {
    return "gateway:lock:idempotency:" + merchantId + ":" + idempotencyKey;
  }
}
