package com.example.payment.gateway.common.exception;

import java.util.Objects;

public class GatewayException extends RuntimeException {

  private final String code;
  private final int statusCode;

  public GatewayException(String code, int statusCode, String message) {
    super(message);
    this.code = Objects.requireNonNull(code, "code must not be null");
    this.statusCode = statusCode;
  }

  public String code() {
    return code;
  }

  public int statusCode() {
    return statusCode;
  }
}
