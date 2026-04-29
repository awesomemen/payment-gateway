package com.example.payment.gateway.application.service;

import com.example.payment.gateway.api.payment.PaymentReconcileResponse;

public interface PaymentReconcileApplicationService {

  PaymentReconcileResponse reconcileProcessingOrders();
}
