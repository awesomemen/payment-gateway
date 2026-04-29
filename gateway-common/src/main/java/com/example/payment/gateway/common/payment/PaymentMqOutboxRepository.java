package com.example.payment.gateway.common.payment;

import java.time.Instant;
import java.util.List;

public interface PaymentMqOutboxRepository {

  List<PaymentMqOutboxRecord> findRetryableFailed(Instant now, int limit);

  void markSent(long id, int retryCount);

  void markFailed(long id, int retryCount, Instant nextRetryTime, String lastErrorMessage);
}
