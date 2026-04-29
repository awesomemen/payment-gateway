package com.example.payment.gateway.application.service;

import com.example.payment.gateway.api.payment.PaymentQueryRequest;
import com.example.payment.gateway.api.payment.PaymentQueryResponse;

public interface PaymentQueryApplicationService {

  PaymentQueryResponse query(PaymentQueryRequest request);
}
