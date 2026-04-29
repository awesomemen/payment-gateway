package com.example.payment.gateway.web.controller;

import com.example.payment.gateway.api.payment.PaymentCallbackRequest;
import com.example.payment.gateway.api.payment.PaymentCallbackResponse;
import com.example.payment.gateway.api.payment.PaymentCreateRequest;
import com.example.payment.gateway.api.payment.PaymentCreateResponse;
import com.example.payment.gateway.api.payment.PaymentQueryRequest;
import com.example.payment.gateway.api.payment.PaymentQueryResponse;
import com.example.payment.gateway.application.service.PaymentCallbackApplicationService;
import com.example.payment.gateway.application.service.PaymentCreateApplicationService;
import com.example.payment.gateway.application.service.PaymentQueryApplicationService;
import com.example.payment.gateway.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

  private final PaymentCreateApplicationService paymentCreateApplicationService;
  private final PaymentQueryApplicationService paymentQueryApplicationService;
  private final PaymentCallbackApplicationService paymentCallbackApplicationService;

  public PaymentController(
      PaymentCreateApplicationService paymentCreateApplicationService,
      PaymentQueryApplicationService paymentQueryApplicationService,
      PaymentCallbackApplicationService paymentCallbackApplicationService
  ) {
    this.paymentCreateApplicationService = paymentCreateApplicationService;
    this.paymentQueryApplicationService = paymentQueryApplicationService;
    this.paymentCallbackApplicationService = paymentCallbackApplicationService;
  }

  @PostMapping
  public ApiResponse<PaymentCreateResponse> create(@Valid @RequestBody PaymentCreateRequest request) {
    return ApiResponse.success(paymentCreateApplicationService.create(request));
  }

  @PostMapping("/query")
  public ApiResponse<PaymentQueryResponse> query(@Valid @RequestBody PaymentQueryRequest request) {
    return ApiResponse.success(paymentQueryApplicationService.query(request));
  }

  @PostMapping("/callback")
  public ApiResponse<PaymentCallbackResponse> callback(@Valid @RequestBody PaymentCallbackRequest request) {
    return ApiResponse.success(paymentCallbackApplicationService.handle(request));
  }
}
