package com.example.payment.gateway.common.payment;

import java.util.List;
import java.util.Optional;

public interface PaymentMessageConsumeRepository {

  void save(PaymentMessageConsumeRecord record);

  Optional<PaymentMessageConsumeRecord> findByMessageKey(String messageKey);

  List<PaymentMessageConsumeRecord> findRetryableFailed(int limit);
}
