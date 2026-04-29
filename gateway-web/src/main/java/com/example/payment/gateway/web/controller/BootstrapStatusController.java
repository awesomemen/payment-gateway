package com.example.payment.gateway.web.controller;

import com.example.payment.gateway.api.system.BootstrapStatusResponse;
import com.example.payment.gateway.application.service.BootstrapStatusApplicationService;
import com.example.payment.gateway.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bootstrap")
public class BootstrapStatusController {

  private final BootstrapStatusApplicationService bootstrapStatusApplicationService;

  public BootstrapStatusController(BootstrapStatusApplicationService bootstrapStatusApplicationService) {
    this.bootstrapStatusApplicationService = bootstrapStatusApplicationService;
  }

  @GetMapping("/status")
  public ApiResponse<BootstrapStatusResponse> status() {
    return ApiResponse.success(bootstrapStatusApplicationService.currentStatus());
  }
}
