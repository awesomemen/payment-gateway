package com.example.payment.gateway.infrastructure.payment;

import com.example.payment.gateway.common.payment.PaymentOrderRecord;
import com.example.payment.gateway.common.payment.PaymentOrderRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("default")
public class InMemoryPaymentOrderRepository implements PaymentOrderRepository {

  private final Map<String, PaymentOrderRecord> records = new ConcurrentHashMap<>();

  @Override
  public void save(PaymentOrderRecord record) {
    records.put(record.gatewayPaymentId(), record);
  }

  @Override
  public Optional<PaymentOrderRecord> findByGatewayPaymentId(String gatewayPaymentId) {
    return Optional.ofNullable(records.get(gatewayPaymentId));
  }

  @Override
  public Optional<PaymentOrderRecord> findByMerchantIdAndRequestId(String merchantId, String requestId) {
    return records.values().stream()
        .filter(record -> merchantId.equals(record.merchantId()) && requestId.equals(record.requestId()))
        .findFirst();
  }

  @Override
  public List<PaymentOrderRecord> findByPaymentStatus(String paymentStatus, int limit) {
    return records.values().stream()
        .filter(record -> paymentStatus.equals(record.paymentStatus()))
        .limit(limit)
        .collect(Collectors.toList());
  }
}
