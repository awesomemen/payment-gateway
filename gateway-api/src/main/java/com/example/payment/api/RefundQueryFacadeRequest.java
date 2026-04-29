package com.example.payment.api;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

public class RefundQueryFacadeRequest implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private String merchantId;
  private String requestId;
  private String gatewayRefundId;
  private String downstreamRefundId;
  private String currentStatus;
  private Instant requestTime;
  private String routeCode;

  public RefundQueryFacadeRequest() {
  }

  public RefundQueryFacadeRequest(
      String merchantId,
      String requestId,
      String gatewayRefundId,
      String downstreamRefundId,
      String currentStatus,
      Instant requestTime,
      String routeCode
  ) {
    this.merchantId = merchantId;
    this.requestId = requestId;
    this.gatewayRefundId = gatewayRefundId;
    this.downstreamRefundId = downstreamRefundId;
    this.currentStatus = currentStatus;
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

  public String requestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public String getRequestId() {
    return requestId;
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

  public String downstreamRefundId() {
    return downstreamRefundId;
  }

  public void setDownstreamRefundId(String downstreamRefundId) {
    this.downstreamRefundId = downstreamRefundId;
  }

  public String getDownstreamRefundId() {
    return downstreamRefundId;
  }

  public String currentStatus() {
    return currentStatus;
  }

  public void setCurrentStatus(String currentStatus) {
    this.currentStatus = currentStatus;
  }

  public String getCurrentStatus() {
    return currentStatus;
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
