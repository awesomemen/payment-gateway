package com.example.payment.gateway.web.advice;

import com.example.payment.gateway.common.exception.GatewayException;
import com.example.payment.gateway.common.response.ApiResponse;
import com.example.payment.gateway.common.response.GatewayResponseCodes;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GatewayExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GatewayExceptionHandler.class);

  @ExceptionHandler(GatewayException.class)
  public ResponseEntity<ApiResponse<Void>> handleGatewayException(GatewayException exception) {
    log.warn("Gateway exception: code={}, status={}, message={}", exception.code(), exception.statusCode(), exception.getMessage());
    return ResponseEntity.status(HttpStatusCode.valueOf(exception.statusCode()))
        .body(ApiResponse.failure(exception.code(), exception.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException exception) {
    String message = exception.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .collect(Collectors.joining(", "));
    return ResponseEntity.badRequest()
        .body(ApiResponse.failure(GatewayResponseCodes.VALIDATION_ERROR, message));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException exception) {
    return ResponseEntity.badRequest()
        .body(ApiResponse.failure(GatewayResponseCodes.VALIDATION_ERROR, exception.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
    log.error("Unexpected request handling error", exception);
    return ResponseEntity.internalServerError()
        .body(ApiResponse.failure(GatewayResponseCodes.INTERNAL_ERROR, "Unexpected internal error"));
  }
}
