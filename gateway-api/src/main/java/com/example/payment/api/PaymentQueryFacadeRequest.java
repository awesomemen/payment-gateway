package com.example.payment.api;

import java.io.Serializable;
import java.time.Instant;

public class PaymentQueryFacadeRequest implements Serializable {

  private String merchantId;
  private String requestId;
  private String gatewayPaymentId;
  private String downstreamPaymentId;
  private String currentStatus;
  private Instant requestTime;

  public PaymentQueryFacadeRequest() {
  }

  public PaymentQueryFacadeRequest(
      String merchantId,
      String requestId,
      String gatewayPaymentId,
      String downstreamPaymentId,
      String currentStatus,
      Instant requestTime
  ) {
    this.merchantId = merchantId;
    this.requestId = requestId;
    this.gatewayPaymentId = gatewayPaymentId;
    this.downstreamPaymentId = downstreamPaymentId;
    this.currentStatus = currentStatus;
    this.requestTime = requestTime;
  }

  public String merchantId() {
    return merchantId;
  }

  public void setMerchantId(String merchantId) {
    this.merchantId = merchantId;
  }

  public String requestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public String gatewayPaymentId() {
    return gatewayPaymentId;
  }

  public void setGatewayPaymentId(String gatewayPaymentId) {
    this.gatewayPaymentId = gatewayPaymentId;
  }

  public String downstreamPaymentId() {
    return downstreamPaymentId;
  }

  public void setDownstreamPaymentId(String downstreamPaymentId) {
    this.downstreamPaymentId = downstreamPaymentId;
  }

  public String currentStatus() {
    return currentStatus;
  }

  public void setCurrentStatus(String currentStatus) {
    this.currentStatus = currentStatus;
  }

  public Instant requestTime() {
    return requestTime;
  }

  public void setRequestTime(Instant requestTime) {
    this.requestTime = requestTime;
  }
}
