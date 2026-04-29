package com.example.payment.gateway.application.service;

import com.example.payment.gateway.api.system.BootstrapStatusResponse;

public interface BootstrapStatusApplicationService {

  BootstrapStatusResponse currentStatus();
}
