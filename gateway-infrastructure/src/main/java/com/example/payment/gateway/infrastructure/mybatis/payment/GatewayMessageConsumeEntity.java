package com.example.payment.gateway.infrastructure.mybatis.payment;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("gateway_message_consume_record")
public class GatewayMessageConsumeEntity {

  private Long id;
  private String messageKey;
  private String bizType;
  private String consumerGroup;
  private String payloadJson;
  private String consumeStatus;
  private Integer retryCount;
  private Integer deadLetter;
  private String lastErrorMessage;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getMessageKey() {
    return messageKey;
  }

  public void setMessageKey(String messageKey) {
    this.messageKey = messageKey;
  }

  public String getBizType() {
    return bizType;
  }

  public void setBizType(String bizType) {
    this.bizType = bizType;
  }

  public String getConsumerGroup() {
    return consumerGroup;
  }

  public void setConsumerGroup(String consumerGroup) {
    this.consumerGroup = consumerGroup;
  }

  public String getPayloadJson() {
    return payloadJson;
  }

  public void setPayloadJson(String payloadJson) {
    this.payloadJson = payloadJson;
  }

  public String getConsumeStatus() {
    return consumeStatus;
  }

  public void setConsumeStatus(String consumeStatus) {
    this.consumeStatus = consumeStatus;
  }

  public Integer getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(Integer retryCount) {
    this.retryCount = retryCount;
  }

  public Integer getDeadLetter() {
    return deadLetter;
  }

  public void setDeadLetter(Integer deadLetter) {
    this.deadLetter = deadLetter;
  }

  public String getLastErrorMessage() {
    return lastErrorMessage;
  }

  public void setLastErrorMessage(String lastErrorMessage) {
    this.lastErrorMessage = lastErrorMessage;
  }
}
