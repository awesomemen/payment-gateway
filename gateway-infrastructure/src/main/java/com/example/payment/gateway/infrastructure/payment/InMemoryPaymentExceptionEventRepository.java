package com.example.payment.gateway.infrastructure.payment;

import com.example.payment.gateway.common.payment.PaymentExceptionEventRecord;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRepository;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("default")
public class InMemoryPaymentExceptionEventRepository implements PaymentExceptionEventRepository {

  private final List<PaymentExceptionEventRecord> records = new CopyOnWriteArrayList<>();

  @Override
  public void save(PaymentExceptionEventRecord record) {
    records.add(record);
  }

  @Override
  public long countAll() {
    return records.size();
  }

  @Override
  public List<PaymentExceptionEventRecord> findRecent(String merchantId, String requestId, int limit) {
    return records.stream()
        .filter(record -> merchantId == null || merchantId.equals(record.merchantId()))
        .filter(record -> requestId == null || requestId.equals(record.requestId()))
        .limit(limit)
        .collect(Collectors.toList());
  }
}
