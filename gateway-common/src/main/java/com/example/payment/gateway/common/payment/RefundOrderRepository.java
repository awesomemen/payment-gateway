package com.example.payment.gateway.common.payment;

import java.util.Optional;

public interface RefundOrderRepository {

  void save(RefundOrderRecord record);

  Optional<RefundOrderRecord> findByGatewayRefundId(String gatewayRefundId);

  Optional<RefundOrderRecord> findByMerchantIdAndRequestId(String merchantId, String requestId);
}
