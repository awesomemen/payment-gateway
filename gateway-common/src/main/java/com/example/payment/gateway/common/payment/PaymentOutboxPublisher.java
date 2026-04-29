package com.example.payment.gateway.common.payment;

public interface PaymentOutboxPublisher {

  void publishPaymentAccepted(PaymentAcceptedOutboxEvent event);
}
