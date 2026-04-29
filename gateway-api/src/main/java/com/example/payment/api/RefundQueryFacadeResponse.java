package com.example.payment.api;

import java.io.Serial;
import java.io.Serializable;

public class RefundQueryFacadeResponse implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private String status;
  private String message;

  public RefundQueryFacadeResponse() {
  }

  public RefundQueryFacadeResponse(String status, String message) {
    this.status = status;
    this.message = message;
  }

  public String status() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getStatus() {
    return status;
  }

  public String message() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
