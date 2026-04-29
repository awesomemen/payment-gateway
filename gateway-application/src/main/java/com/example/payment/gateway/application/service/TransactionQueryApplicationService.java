package com.example.payment.gateway.application.service;

import com.example.payment.gateway.api.payment.TransactionDetailRequest;
import com.example.payment.gateway.api.payment.TransactionDetailResponse;

public interface TransactionQueryApplicationService {

  TransactionDetailResponse query(TransactionDetailRequest request);
}
