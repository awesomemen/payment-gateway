package com.example.payment.gateway.infrastructure.payment;

import com.example.payment.gateway.common.payment.PaymentRequestLogRecord;
import com.example.payment.gateway.common.payment.PaymentRequestLogRepository;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("default")
public class InMemoryPaymentRequestLogRepository implements PaymentRequestLogRepository {

  private final List<PaymentRequestLogRecord> records = new CopyOnWriteArrayList<>();

  @Override
  public void save(PaymentRequestLogRecord record) {
    records.add(record);
  }

  @Override
  public long countByResponseStatus(String responseStatus) {
    return records.stream()
        .filter(record -> responseStatus.equals(record.responseStatus()))
        .count();
  }

  @Override
  public List<PaymentRequestLogRecord> findRecent(String merchantId, String requestId, int limit) {
    return records.stream()
        .filter(record -> merchantId == null || merchantId.equals(record.merchantId()))
        .filter(record -> requestId == null || requestId.equals(record.requestId()))
        .limit(limit)
        .collect(Collectors.toList());
  }
}
