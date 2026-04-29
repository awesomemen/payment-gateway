package com.example.payment.api;

import java.io.Serial;
import java.io.Serializable;

public class RefundCreateFacadeResponse implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private String downstreamRefundId;
  private String status;
  private String message;

  public RefundCreateFacadeResponse() {
  }

  public RefundCreateFacadeResponse(String downstreamRefundId, String status, String message) {
    this.downstreamRefundId = downstreamRefundId;
    this.status = status;
    this.message = message;
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
