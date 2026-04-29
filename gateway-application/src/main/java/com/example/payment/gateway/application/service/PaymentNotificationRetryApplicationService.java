package com.example.payment.gateway.application.service;

import com.example.payment.gateway.api.payment.PaymentNotificationRetryResponse;

public interface PaymentNotificationRetryApplicationService {

  PaymentNotificationRetryResponse retryFailedNotifications();
}
