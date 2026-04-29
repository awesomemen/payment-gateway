package com.example.payment.gateway.infrastructure.mybatis.payment;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("gateway_refund_order")
public class GatewayRefundOrderEntity {

  private Long id;
  private String gatewayRefundId;
  private String merchantId;
  private String requestId;
  private String gatewayPaymentId;
  private String idempotencyKey;
  private String routeCode;
  private String targetService;
  private String downstreamRefundId;
  private String refundStatus;
  private String amount;
  private String currency;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getGatewayRefundId() {
    return gatewayRefundId;
  }

  public void setGatewayRefundId(String gatewayRefundId) {
    this.gatewayRefundId = gatewayRefundId;
  }

  public String getMerchantId() {
    return merchantId;
  }

  public void setMerchantId(String merchantId) {
    this.merchantId = merchantId;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public String getGatewayPaymentId() {
    return gatewayPaymentId;
  }

  public void setGatewayPaymentId(String gatewayPaymentId) {
    this.gatewayPaymentId = gatewayPaymentId;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public void setIdempotencyKey(String idempotencyKey) {
    this.idempotencyKey = idempotencyKey;
  }

  public String getRouteCode() {
    return routeCode;
  }

  public void setRouteCode(String routeCode) {
    this.routeCode = routeCode;
  }

  public String getTargetService() {
    return targetService;
  }

  public void setTargetService(String targetService) {
    this.targetService = targetService;
  }

  public String getDownstreamRefundId() {
    return downstreamRefundId;
  }

  public void setDownstreamRefundId(String downstreamRefundId) {
    this.downstreamRefundId = downstreamRefundId;
  }

  public String getRefundStatus() {
    return refundStatus;
  }

  public void setRefundStatus(String refundStatus) {
    this.refundStatus = refundStatus;
  }

  public String getAmount() {
    return amount;
  }

  public void setAmount(String amount) {
    this.amount = amount;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }
}
