package com.example.payment.gateway.application.service;

import com.example.payment.gateway.api.payment.PaymentOutboxRetryResponse;

public interface PaymentOutboxRetryApplicationService {

  PaymentOutboxRetryResponse retryFailedMessages();
}
