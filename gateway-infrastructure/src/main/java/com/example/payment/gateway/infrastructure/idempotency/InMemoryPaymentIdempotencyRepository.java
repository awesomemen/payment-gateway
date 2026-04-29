package com.example.payment.gateway.infrastructure.idempotency;

import com.example.payment.gateway.common.idempotency.PaymentIdempotencyRecord;
import com.example.payment.gateway.common.idempotency.PaymentIdempotencyRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("default")
public class InMemoryPaymentIdempotencyRepository implements PaymentIdempotencyRepository {

  private final ConcurrentHashMap<String, ExpiringRecord> records = new ConcurrentHashMap<>();
  private final Clock clock;

  public InMemoryPaymentIdempotencyRepository(Clock clock) {
    this.clock = clock;
  }

  @Override
  public Optional<PaymentIdempotencyRecord> find(String merchantId, String idempotencyKey) {
    Instant now = Instant.now(clock);
    String key = cacheKey(merchantId, idempotencyKey);
    ExpiringRecord expiringRecord = records.get(key);
    if (expiringRecord == null) {
      return Optional.empty();
    }
    if (!expiringRecord.expiresAt().isAfter(now)) {
      records.remove(key);
      return Optional.empty();
    }
    return Optional.of(expiringRecord.record());
  }

  @Override
  public void save(PaymentIdempotencyRecord record, Duration ttl) {
    records.put(
        cacheKey(record.merchantId(), record.idempotencyKey()),
        new ExpiringRecord(record, Instant.now(clock).plus(ttl))
    );
  }

  private static String cacheKey(String merchantId, String idempotencyKey) {
    return merchantId + ":" + idempotencyKey;
  }

  private record ExpiringRecord(PaymentIdempotencyRecord record, Instant expiresAt) {
  }
}
