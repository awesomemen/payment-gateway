package com.example.payment.api;

import java.io.Serial;
import java.io.Serializable;

public class PaymentCreateFacadeResponse implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private String downstreamPaymentId;
  private String status;
  private String message;

  public PaymentCreateFacadeResponse() {
  }

  public PaymentCreateFacadeResponse(String downstreamPaymentId, String status, String message) {
    this.downstreamPaymentId = downstreamPaymentId;
    this.status = status;
    this.message = message;
  }

  public String getDownstreamPaymentId() {
    return downstreamPaymentId;
  }

  public void setDownstreamPaymentId(String downstreamPaymentId) {
    this.downstreamPaymentId = downstreamPaymentId;
  }

  public String downstreamPaymentId() {
    return downstreamPaymentId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String status() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String message() {
    return message;
  }
}
