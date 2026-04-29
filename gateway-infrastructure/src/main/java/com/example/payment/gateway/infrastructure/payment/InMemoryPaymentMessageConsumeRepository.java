package com.example.payment.gateway.infrastructure.payment;

import com.example.payment.gateway.common.payment.PaymentMessageConsumeRecord;
import com.example.payment.gateway.common.payment.PaymentMessageConsumeRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("default")
public class InMemoryPaymentMessageConsumeRepository implements PaymentMessageConsumeRepository {

  private final Map<String, PaymentMessageConsumeRecord> records = new ConcurrentHashMap<>();

  @Override
  public void save(PaymentMessageConsumeRecord record) {
    records.put(record.messageKey(), record);
  }

  @Override
  public Optional<PaymentMessageConsumeRecord> findByMessageKey(String messageKey) {
    return Optional.ofNullable(records.get(messageKey));
  }

  @Override
  public List<PaymentMessageConsumeRecord> findRetryableFailed(int limit) {
    return records.values().stream()
        .filter(record -> "FAILED".equals(record.consumeStatus()))
        .limit(limit)
        .collect(Collectors.toList());
  }
}
