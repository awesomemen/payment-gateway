package com.example.payment.gateway.web.controller;

import com.example.payment.gateway.api.payment.AuditSearchResponse;
import com.example.payment.gateway.api.payment.TransactionDetailRequest;
import com.example.payment.gateway.api.payment.TransactionDetailResponse;
import com.example.payment.gateway.application.service.AuditSearchApplicationService;
import com.example.payment.gateway.application.service.TransactionQueryApplicationService;
import com.example.payment.gateway.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

  private final TransactionQueryApplicationService transactionQueryApplicationService;
  private final AuditSearchApplicationService auditSearchApplicationService;

  public TransactionController(
      TransactionQueryApplicationService transactionQueryApplicationService,
      AuditSearchApplicationService auditSearchApplicationService
  ) {
    this.transactionQueryApplicationService = transactionQueryApplicationService;
    this.auditSearchApplicationService = auditSearchApplicationService;
  }

  @PostMapping("/detail")
  public ApiResponse<TransactionDetailResponse> detail(@Valid @RequestBody TransactionDetailRequest request) {
    return ApiResponse.success(transactionQueryApplicationService.query(request));
  }

  @GetMapping("/audit")
  public ApiResponse<AuditSearchResponse> audit(
      @RequestParam(required = false) String merchantId,
      @RequestParam(required = false) String requestId,
      @RequestParam(defaultValue = "20") int limit
  ) {
    return ApiResponse.success(auditSearchApplicationService.search(merchantId, requestId, limit));
  }
}
