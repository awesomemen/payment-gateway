package com.example.payment.gateway.common.response;

public record ApiResponse<T>(String code, String message, T data) {

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(GatewayResponseCodes.SUCCESS, "OK", data);
  }

  public static <T> ApiResponse<T> failure(String code, String message) {
    return new ApiResponse<>(code, message, null);
  }
}
