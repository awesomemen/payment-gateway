package com.example.payment.gateway.common.payment;

import java.util.List;

public interface PaymentRequestLogRepository {

  void save(PaymentRequestLogRecord record);

  long countByResponseStatus(String responseStatus);

  List<PaymentRequestLogRecord> findRecent(String merchantId, String requestId, int limit);
}
