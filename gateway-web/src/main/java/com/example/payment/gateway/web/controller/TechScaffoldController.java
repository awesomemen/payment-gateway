package com.example.payment.gateway.web.controller;

import com.example.payment.gateway.api.tech.TechScaffoldStatusResponse;
import com.example.payment.gateway.application.service.TechScaffoldApplicationService;
import com.example.payment.gateway.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tech")
public class TechScaffoldController {

  private final TechScaffoldApplicationService techScaffoldApplicationService;

  public TechScaffoldController(TechScaffoldApplicationService techScaffoldApplicationService) {
    this.techScaffoldApplicationService = techScaffoldApplicationService;
  }

  @GetMapping("/status")
  public ApiResponse<TechScaffoldStatusResponse> status() {
    return ApiResponse.success(techScaffoldApplicationService.currentStatus());
  }
}
