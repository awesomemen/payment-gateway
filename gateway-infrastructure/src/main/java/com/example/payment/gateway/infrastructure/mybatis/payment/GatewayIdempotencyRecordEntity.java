package com.example.payment.gateway.infrastructure.mybatis.payment;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("gateway_idempotency_record")
public class GatewayIdempotencyRecordEntity {

  private Long id;
  private String idempotencyKey;
  private String merchantId;
  private String requestId;
  private String bizType;
  private String requestHash;
  private Integer processStatus;
  private String responseCode;
  private String responseMessage;
  private String resultGatewayPaymentId;
  private String resultStatus;
  private String resultRouteCode;
  private LocalDateTime resultProcessedAt;
  private LocalDateTime expireAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public void setIdempotencyKey(String idempotencyKey) {
    this.idempotencyKey = idempotencyKey;
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

  public String getBizType() {
    return bizType;
  }

  public void setBizType(String bizType) {
    this.bizType = bizType;
  }

  public String getRequestHash() {
    return requestHash;
  }

  public void setRequestHash(String requestHash) {
    this.requestHash = requestHash;
  }

  public Integer getProcessStatus() {
    return processStatus;
  }

  public void setProcessStatus(Integer processStatus) {
    this.processStatus = processStatus;
  }

  public String getResponseCode() {
    return responseCode;
  }

  public void setResponseCode(String responseCode) {
    this.responseCode = responseCode;
  }

  public String getResponseMessage() {
    return responseMessage;
  }

  public void setResponseMessage(String responseMessage) {
    this.responseMessage = responseMessage;
  }

  public String getResultGatewayPaymentId() {
    return resultGatewayPaymentId;
  }

  public void setResultGatewayPaymentId(String resultGatewayPaymentId) {
    this.resultGatewayPaymentId = resultGatewayPaymentId;
  }

  public String getResultStatus() {
    return resultStatus;
  }

  public void setResultStatus(String resultStatus) {
    this.resultStatus = resultStatus;
  }

  public String getResultRouteCode() {
    return resultRouteCode;
  }

  public void setResultRouteCode(String resultRouteCode) {
    this.resultRouteCode = resultRouteCode;
  }

  public LocalDateTime getResultProcessedAt() {
    return resultProcessedAt;
  }

  public void setResultProcessedAt(LocalDateTime resultProcessedAt) {
    this.resultProcessedAt = resultProcessedAt;
  }

  public LocalDateTime getExpireAt() {
    return expireAt;
  }

  public void setExpireAt(LocalDateTime expireAt) {
    this.expireAt = expireAt;
  }
}
