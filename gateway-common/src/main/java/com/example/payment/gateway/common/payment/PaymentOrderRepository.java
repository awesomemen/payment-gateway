package com.example.payment.gateway.common.payment;

import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository {

  void save(PaymentOrderRecord record);

  Optional<PaymentOrderRecord> findByGatewayPaymentId(String gatewayPaymentId);

  Optional<PaymentOrderRecord> findByMerchantIdAndRequestId(String merchantId, String requestId);

  List<PaymentOrderRecord> findByPaymentStatus(String paymentStatus, int limit);
}
