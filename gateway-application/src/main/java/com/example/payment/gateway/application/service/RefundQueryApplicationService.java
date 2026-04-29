package com.example.payment.gateway.application.service;

import com.example.payment.gateway.api.payment.RefundQueryRequest;
import com.example.payment.gateway.api.payment.RefundQueryResponse;

public interface RefundQueryApplicationService {

  RefundQueryResponse query(RefundQueryRequest request);
}
