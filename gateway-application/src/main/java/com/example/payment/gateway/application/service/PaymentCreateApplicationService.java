package com.example.payment.gateway.application.service;

import com.example.payment.gateway.api.payment.PaymentCreateRequest;
import com.example.payment.gateway.api.payment.PaymentCreateResponse;

public interface PaymentCreateApplicationService {

  PaymentCreateResponse create(PaymentCreateRequest request);
}
