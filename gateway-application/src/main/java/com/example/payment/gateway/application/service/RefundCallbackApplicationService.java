package com.example.payment.gateway.application.service;

import com.example.payment.gateway.api.payment.RefundCallbackRequest;
import com.example.payment.gateway.api.payment.RefundCallbackResponse;

public interface RefundCallbackApplicationService {

  RefundCallbackResponse handle(RefundCallbackRequest request);
}
