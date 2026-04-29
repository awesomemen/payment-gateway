package com.example.payment.gateway.infrastructure.mybatis.payment;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("gateway_route_config")
public class GatewayRouteConfigEntity {

  private Long id;
  private String routeCode;
  private String bizType;
  private String apiCode;
  private String targetProtocol;
  private String targetService;
  private String targetMethod;
  private String targetVersion;
  private Integer timeoutMs;
  private Integer retryTimes;
  private String sentinelResource;
  private Integer status;
  private Integer priority;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getRouteCode() {
    return routeCode;
  }

  public void setRouteCode(String routeCode) {
    this.routeCode = routeCode;
  }

  public String getBizType() {
    return bizType;
  }

  public void setBizType(String bizType) {
    this.bizType = bizType;
  }

  public String getApiCode() {
    return apiCode;
  }

  public void setApiCode(String apiCode) {
    this.apiCode = apiCode;
  }

  public String getTargetProtocol() {
    return targetProtocol;
  }

  public void setTargetProtocol(String targetProtocol) {
    this.targetProtocol = targetProtocol;
  }

  public String getTargetService() {
    return targetService;
  }

  public void setTargetService(String targetService) {
    this.targetService = targetService;
  }

  public String getTargetMethod() {
    return targetMethod;
  }

  public void setTargetMethod(String targetMethod) {
    this.targetMethod = targetMethod;
  }

  public String getTargetVersion() {
    return targetVersion;
  }

  public void setTargetVersion(String targetVersion) {
    this.targetVersion = targetVersion;
  }

  public Integer getTimeoutMs() {
    return timeoutMs;
  }

  public void setTimeoutMs(Integer timeoutMs) {
    this.timeoutMs = timeoutMs;
  }

  public Integer getRetryTimes() {
    return retryTimes;
  }

  public void setRetryTimes(Integer retryTimes) {
    this.retryTimes = retryTimes;
  }

  public String getSentinelResource() {
    return sentinelResource;
  }

  public void setSentinelResource(String sentinelResource) {
    this.sentinelResource = sentinelResource;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public Integer getPriority() {
    return priority;
  }

  public void setPriority(Integer priority) {
    this.priority = priority;
  }
}
