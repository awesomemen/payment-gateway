package com.example.payment.gateway.common.idempotency;

import java.time.Duration;
import java.util.Optional;

public interface PaymentIdempotencyRepository {

  Optional<PaymentIdempotencyRecord> find(String merchantId, String idempotencyKey);

  void save(PaymentIdempotencyRecord record, Duration ttl);
}
