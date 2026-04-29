package com.example.payment.gateway.web.controller;

import com.example.payment.gateway.api.payment.RefundCallbackRequest;
import com.example.payment.gateway.api.payment.RefundCallbackResponse;
import com.example.payment.gateway.api.payment.RefundCreateRequest;
import com.example.payment.gateway.api.payment.RefundCreateResponse;
import com.example.payment.gateway.api.payment.RefundQueryRequest;
import com.example.payment.gateway.api.payment.RefundQueryResponse;
import com.example.payment.gateway.application.service.RefundCallbackApplicationService;
import com.example.payment.gateway.application.service.RefundCreateApplicationService;
import com.example.payment.gateway.application.service.RefundQueryApplicationService;
import com.example.payment.gateway.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/refunds")
public class RefundController {

  private final RefundCreateApplicationService refundCreateApplicationService;
  private final RefundQueryApplicationService refundQueryApplicationService;
  private final RefundCallbackApplicationService refundCallbackApplicationService;

  public RefundController(
      RefundCreateApplicationService refundCreateApplicationService,
      RefundQueryApplicationService refundQueryApplicationService,
      RefundCallbackApplicationService refundCallbackApplicationService
  ) {
    this.refundCreateApplicationService = refundCreateApplicationService;
    this.refundQueryApplicationService = refundQueryApplicationService;
    this.refundCallbackApplicationService = refundCallbackApplicationService;
  }

  @PostMapping
  public ApiResponse<RefundCreateResponse> create(@Valid @RequestBody RefundCreateRequest request) {
    return ApiResponse.success(refundCreateApplicationService.create(request));
  }

  @PostMapping("/query")
  public ApiResponse<RefundQueryResponse> query(@Valid @RequestBody RefundQueryRequest request) {
    return ApiResponse.success(refundQueryApplicationService.query(request));
  }

  @PostMapping("/callback")
  public ApiResponse<RefundCallbackResponse> callback(@Valid @RequestBody RefundCallbackRequest request) {
    return ApiResponse.success(refundCallbackApplicationService.handle(request));
  }
}
