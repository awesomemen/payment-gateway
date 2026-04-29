package com.example.payment.api;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

public class PaymentCreateFacadeRequest implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private String merchantId;
  private String gatewayPaymentId;
  private String requestId;
  private String idempotencyKey;
  private String amount;
  private String currency;
  private Instant requestTime;
  private String routeCode;

  public PaymentCreateFacadeRequest() {
  }

  public PaymentCreateFacadeRequest(
      String merchantId,
      String gatewayPaymentId,
      String requestId,
      String idempotencyKey,
      String amount,
      String currency,
      Instant requestTime,
      String routeCode
  ) {
    this.merchantId = merchantId;
    this.gatewayPaymentId = gatewayPaymentId;
    this.requestId = requestId;
    this.idempotencyKey = idempotencyKey;
    this.amount = amount;
    this.currency = currency;
    this.requestTime = requestTime;
    this.routeCode = routeCode;
  }

  public String getMerchantId() {
    return merchantId;
  }

  public void setMerchantId(String merchantId) {
    this.merchantId = merchantId;
  }

  public String merchantId() {
    return merchantId;
  }

  public String getGatewayPaymentId() {
    return gatewayPaymentId;
  }

  public void setGatewayPaymentId(String gatewayPaymentId) {
    this.gatewayPaymentId = gatewayPaymentId;
  }

  public String gatewayPaymentId() {
    return gatewayPaymentId;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public String requestId() {
    return requestId;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public void setIdempotencyKey(String idempotencyKey) {
    this.idempotencyKey = idempotencyKey;
  }

  public String idempotencyKey() {
    return idempotencyKey;
  }

  public String getAmount() {
    return amount;
  }

  public void setAmount(String amount) {
    this.amount = amount;
  }

  public String amount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public String currency() {
    return currency;
  }

  public Instant getRequestTime() {
    return requestTime;
  }

  public void setRequestTime(Instant requestTime) {
    this.requestTime = requestTime;
  }

  public Instant requestTime() {
    return requestTime;
  }

  public String getRouteCode() {
    return routeCode;
  }

  public void setRouteCode(String routeCode) {
    this.routeCode = routeCode;
  }

  public String routeCode() {
    return routeCode;
  }
}
