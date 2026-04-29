package com.example.payment.gateway.infrastructure.idempotency;

import com.example.payment.gateway.common.idempotency.PaymentIdempotencyRecord;
import com.example.payment.gateway.common.idempotency.PaymentIdempotencyRepository;
import com.example.payment.gateway.common.payment.PaymentBizTypes;
import com.example.payment.gateway.infrastructure.payment.JdbcPaymentIdempotencyJournalRepository;
import java.time.Duration;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Primary
@Repository
@Profile({"local", "docker"})
public class CompositePaymentIdempotencyRepository implements PaymentIdempotencyRepository {

  private final RedisPaymentIdempotencyRepository redisRepository;
  private final JdbcPaymentIdempotencyJournalRepository jdbcJournalRepository;

  public CompositePaymentIdempotencyRepository(
      RedisPaymentIdempotencyRepository redisRepository,
      JdbcPaymentIdempotencyJournalRepository jdbcJournalRepository
  ) {
    this.redisRepository = redisRepository;
    this.jdbcJournalRepository = jdbcJournalRepository;
  }

  @Override
  public Optional<PaymentIdempotencyRecord> find(String merchantId, String idempotencyKey) {
    Optional<PaymentIdempotencyRecord> cached = redisRepository.find(merchantId, idempotencyKey);
    if (cached.isPresent()) {
      return cached;
    }
    Optional<PaymentIdempotencyRecord> persisted = jdbcJournalRepository.find(merchantId, idempotencyKey);
    persisted.ifPresent(record -> redisRepository.save(record, Duration.ofHours(1)));
    return persisted;
  }

  @Override
  public void save(PaymentIdempotencyRecord record, Duration ttl) {
    redisRepository.save(record, ttl);
    String requestId = requestIdFromFingerprint(record.requestFingerprint());
    jdbcJournalRepository.save(record, ttl, requestId, bizTypeFromFingerprint(record.requestFingerprint()));
  }

  private static String requestIdFromFingerprint(String fingerprint) {
    for (String token : fingerprint.split("&")) {
      if (token.startsWith("requestId=")) {
        return token.substring("requestId=".length());
      }
    }
    return "UNKNOWN";
  }

  private static String bizTypeFromFingerprint(String fingerprint) {
    for (String token : fingerprint.split("&")) {
      if (token.startsWith("bizType=")) {
        return token.substring("bizType=".length());
      }
    }
    return PaymentBizTypes.PAY;
  }
}
