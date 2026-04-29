package com.example.payment.gateway.common.payment;

public interface PaymentOutboxRetryExecutor {

  void send(PaymentMqOutboxRecord record);
}
