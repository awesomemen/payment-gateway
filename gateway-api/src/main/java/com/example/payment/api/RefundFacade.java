package com.example.payment.api;

public interface RefundFacade {

  RefundCreateFacadeResponse createRefund(RefundCreateFacadeRequest request);

  RefundQueryFacadeResponse queryRefund(RefundQueryFacadeRequest request);
}
