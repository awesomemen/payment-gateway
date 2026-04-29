package com.example.payment.gateway.infrastructure.payment;

import com.example.payment.gateway.common.payment.RefundOrderRecord;
import com.example.payment.gateway.common.payment.RefundOrderRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("default")
public class InMemoryRefundOrderRepository implements RefundOrderRepository {

  private final Map<String, RefundOrderRecord> records = new ConcurrentHashMap<>();

  @Override
  public void save(RefundOrderRecord record) {
    records.put(record.gatewayRefundId(), record);
  }

  @Override
  public Optional<RefundOrderRecord> findByGatewayRefundId(String gatewayRefundId) {
    return Optional.ofNullable(records.get(gatewayRefundId));
  }

  @Override
  public Optional<RefundOrderRecord> findByMerchantIdAndRequestId(String merchantId, String requestId) {
    return records.values().stream()
        .filter(record -> merchantId.equals(record.merchantId()) && requestId.equals(record.requestId()))
        .findFirst();
  }
}
