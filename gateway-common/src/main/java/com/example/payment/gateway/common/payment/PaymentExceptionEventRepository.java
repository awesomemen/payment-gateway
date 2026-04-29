package com.example.payment.gateway.common.payment;

import java.util.List;

public interface PaymentExceptionEventRepository {

  void save(PaymentExceptionEventRecord record);

  long countAll();

  List<PaymentExceptionEventRecord> findRecent(String merchantId, String requestId, int limit);
}
