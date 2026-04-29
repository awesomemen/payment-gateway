package com.example.payment.gateway.application.service;

import com.example.payment.gateway.api.payment.PaymentGovernanceConfigResponse;

public interface PaymentGovernanceQueryApplicationService {

  PaymentGovernanceConfigResponse currentConfig();
}
