package com.example.payment.gateway.application.service;

import com.example.payment.gateway.api.payment.PaymentCallbackRequest;
import com.example.payment.gateway.api.payment.PaymentCallbackResponse;

public interface PaymentCallbackApplicationService {

  PaymentCallbackResponse handle(PaymentCallbackRequest request);
}
