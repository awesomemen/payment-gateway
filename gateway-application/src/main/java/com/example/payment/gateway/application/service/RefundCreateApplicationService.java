package com.example.payment.gateway.application.service;

import com.example.payment.gateway.api.payment.RefundCreateRequest;
import com.example.payment.gateway.api.payment.RefundCreateResponse;

public interface RefundCreateApplicationService {

  RefundCreateResponse create(RefundCreateRequest request);
}
