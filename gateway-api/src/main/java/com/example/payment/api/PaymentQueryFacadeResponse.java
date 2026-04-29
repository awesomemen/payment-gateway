package com.example.payment.api;

import java.io.Serializable;

public class PaymentQueryFacadeResponse implements Serializable {

  private String status;
  private String message;

  public PaymentQueryFacadeResponse() {
  }

  public PaymentQueryFacadeResponse(String status, String message) {
    this.status = status;
    this.message = message;
  }

  public String status() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String message() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
