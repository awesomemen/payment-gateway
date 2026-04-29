package com.example.payment.gateway.common.response;

public final class GatewayResponseCodes {

  public static final String SUCCESS = "SUCCESS";
  public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
  public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
  public static final String MERCHANT_NOT_FOUND = "MERCHANT_NOT_FOUND";
  public static final String SIGNATURE_INVALID = "SIGNATURE_INVALID";
  public static final String REQUEST_EXPIRED = "REQUEST_EXPIRED";
  public static final String REQUEST_REPLAYED = "REQUEST_REPLAYED";
  public static final String REPLAY_ATTACK_DETECTED = "REPLAY_ATTACK_DETECTED";
  public static final String IDEMPOTENCY_CONFLICT = "IDEMPOTENCY_CONFLICT";
  public static final String IDEMPOTENCY_IN_PROGRESS = "IDEMPOTENCY_IN_PROGRESS";
  public static final String RATE_LIMITED = "RATE_LIMITED";
  public static final String ROUTE_NOT_FOUND = "ROUTE_NOT_FOUND";
  public static final String UNSUPPORTED_ROUTE_PROTOCOL = "UNSUPPORTED_ROUTE_PROTOCOL";
  public static final String DOWNSTREAM_TIMEOUT = "DOWNSTREAM_TIMEOUT";
  public static final String DOWNSTREAM_SERVICE_ERROR = "DOWNSTREAM_SERVICE_ERROR";
  public static final String DOWNSTREAM_EMPTY_RESPONSE = "DOWNSTREAM_EMPTY_RESPONSE";
  public static final String DOWNSTREAM_REJECTED = "DOWNSTREAM_REJECTED";
  public static final String DOWNSTREAM_FAILED = "DOWNSTREAM_FAILED";
  public static final String OUTBOX_RETRY_FAILED = "OUTBOX_RETRY_FAILED";
  public static final String MESSAGE_CONSUME_FAILED = "MESSAGE_CONSUME_FAILED";
  public static final String PAYMENT_CREATE_NOT_READY = "PAYMENT_CREATE_NOT_READY";
  public static final String PAYMENT_ORDER_NOT_FOUND = "PAYMENT_ORDER_NOT_FOUND";
  public static final String PAYMENT_ORDER_MISMATCH = "PAYMENT_ORDER_MISMATCH";
  public static final String PAYMENT_STATUS_CONFLICT = "PAYMENT_STATUS_CONFLICT";
  public static final String REFUND_ORDER_NOT_FOUND = "REFUND_ORDER_NOT_FOUND";
  public static final String REFUND_ORDER_MISMATCH = "REFUND_ORDER_MISMATCH";
  public static final String REFUND_STATUS_CONFLICT = "REFUND_STATUS_CONFLICT";

  private GatewayResponseCodes() {
  }
}
