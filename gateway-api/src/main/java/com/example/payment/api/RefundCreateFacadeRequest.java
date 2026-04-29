package com.example.payment.api;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

public class RefundCreateFacadeRequest implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private String merchantId;
  private String gatewayRefundId;
  private String gatewayPaymentId;
  private String requestId;
  private String idempotencyKey;
  private String amount;
  private String currency;
  private Instant requestTime;
  private String routeCode;

  public RefundCreateFacadeRequest() {
  }

  public RefundCreateFacadeRequest(
      String merchantId,
      String gatewayRefundId,
      String gatewayPaymentId,
      String requestId,
      String idempotencyKey,
      String amount,
      String currency,
      Instant requestTime,
      String routeCode
  ) {
    this.merchantId = merchantId;
    this.gatewayRefundId = gatewayRefundId;
    this.gatewayPaymentId = gatewayPaymentId;
    this.requestId = requestId;
    this.idempotencyKey = idempotencyKey;
    this.amount = amount;
    this.currency = currency;
    this.requestTime = requestTime;
    this.routeCode = routeCode;
  }

  public String merchantId() {
    return merchantId;
  }

  public void setMerchantId(String merchantId) {
    this.merchantId = merchantId;
  }

  public String getMerchantId() {
    return merchantId;
  }

  public String gatewayRefundId() {
    return gatewayRefundId;
  }

  public void setGatewayRefundId(String gatewayRefundId) {
    this.gatewayRefundId = gatewayRefundId;
  }

  public String getGatewayRefundId() {
    return gatewayRefundId;
  }

  public String gatewayPaymentId() {
    return gatewayPaymentId;
  }

  public void setGatewayPaymentId(String gatewayPaymentId) {
    this.gatewayPaymentId = gatewayPaymentId;
  }

  public String getGatewayPaymentId() {
    return gatewayPaymentId;
  }

  public String requestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public String getRequestId() {
    return requestId;
  }

  public String idempotencyKey() {
    return idempotencyKey;
  }

  public void setIdempotencyKey(String idempotencyKey) {
    this.idempotencyKey = idempotencyKey;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public String amount() {
    return amount;
  }

  public void setAmount(String amount) {
    this.amount = amount;
  }

  public String getAmount() {
    return amount;
  }

  public String currency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public String getCurrency() {
    return currency;
  }

  public Instant requestTime() {
    return requestTime;
  }

  public void setRequestTime(Instant requestTime) {
    this.requestTime = requestTime;
  }

  public Instant getRequestTime() {
    return requestTime;
  }

  public String routeCode() {
    return routeCode;
  }

  public void setRouteCode(String routeCode) {
    this.routeCode = routeCode;
  }

  public String getRouteCode() {
    return routeCode;
  }
}
