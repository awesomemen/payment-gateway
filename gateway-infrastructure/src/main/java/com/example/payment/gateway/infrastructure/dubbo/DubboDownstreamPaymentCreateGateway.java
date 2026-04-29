package com.example.payment.gateway.infrastructure.dubbo;

import com.example.payment.api.PaymentCreateFacade;
import com.example.payment.api.PaymentCreateFacadeRequest;
import com.example.payment.api.PaymentCreateFacadeResponse;
import com.example.payment.gateway.common.exception.GatewayException;
import com.example.payment.gateway.common.payment.DownstreamPaymentCreateGateway;
import com.example.payment.gateway.common.payment.DownstreamPaymentCreateRequest;
import com.example.payment.gateway.common.payment.DownstreamPaymentCreateResult;
import com.example.payment.gateway.common.payment.GatewayRouteDefinition;
import com.example.payment.gateway.common.response.GatewayResponseCodes;
import java.util.Locale;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "docker"})
public class DubboDownstreamPaymentCreateGateway implements DownstreamPaymentCreateGateway {

  private final PaymentCreateFacadeMapper paymentCreateFacadeMapper;

  @DubboReference(interfaceClass = PaymentCreateFacade.class, version = "1.0.0", check = false, timeout = 3000, retries = 0, injvm = false)
  private PaymentCreateFacade paymentCreateFacade;

  @Autowired
  public DubboDownstreamPaymentCreateGateway(PaymentCreateFacadeMapper paymentCreateFacadeMapper) {
    this.paymentCreateFacadeMapper = paymentCreateFacadeMapper;
  }

  DubboDownstreamPaymentCreateGateway(
      PaymentCreateFacade paymentCreateFacade,
      PaymentCreateFacadeMapper paymentCreateFacadeMapper
  ) {
    this.paymentCreateFacade = paymentCreateFacade;
    this.paymentCreateFacadeMapper = paymentCreateFacadeMapper;
  }

  @Override
  public DownstreamPaymentCreateResult create(
      GatewayRouteDefinition route,
      String gatewayPaymentId,
      DownstreamPaymentCreateRequest request
  ) {
    try {
      PaymentCreateFacadeResponse response = paymentCreateFacade.createPayment(
          paymentCreateFacadeMapper.toFacadeRequest(route, gatewayPaymentId, request)
      );
      if (response == null) {
        throw new GatewayException(
            GatewayResponseCodes.DOWNSTREAM_EMPTY_RESPONSE,
            502,
            "Downstream payment create facade returned empty response"
        );
      }
      return mapResponse(response);
    } catch (RpcException exception) {
      if (isTimeoutException(exception)) {
        throw new GatewayException(
            GatewayResponseCodes.DOWNSTREAM_TIMEOUT,
            504,
            "Downstream payment create timed out"
        );
      }
      throw new GatewayException(
          GatewayResponseCodes.DOWNSTREAM_SERVICE_ERROR,
          502,
          "Downstream payment create invocation failed"
      );
    }
  }

  private static DownstreamPaymentCreateResult mapResponse(PaymentCreateFacadeResponse response) {
    String normalizedStatus = normalizeStatus(response.status());
    return switch (normalizedStatus) {
      case "ACCEPTED", "PROCESSING" -> new DownstreamPaymentCreateResult(
          response.downstreamPaymentId(),
          normalizedStatus,
          defaultMessage(response.message(), normalizedStatus)
      );
      case "REJECTED" -> throw new GatewayException(
          GatewayResponseCodes.DOWNSTREAM_REJECTED,
          422,
          defaultMessage(response.message(), "Downstream payment request was rejected")
      );
      case "FAILED", "FAIL", "ERROR" -> throw new GatewayException(
          GatewayResponseCodes.DOWNSTREAM_FAILED,
          502,
          defaultMessage(response.message(), "Downstream payment request failed")
      );
      default -> throw new GatewayException(
          GatewayResponseCodes.DOWNSTREAM_SERVICE_ERROR,
          502,
          "Unsupported downstream payment status: " + response.status()
      );
    };
  }

  private static String normalizeStatus(String status) {
    if (status == null || status.isBlank()) {
      throw new GatewayException(
          GatewayResponseCodes.DOWNSTREAM_EMPTY_RESPONSE,
          502,
          "Downstream payment create facade returned empty status"
      );
    }
    return status.trim().toUpperCase(Locale.ROOT);
  }

  private static String defaultMessage(String message, String fallbackMessage) {
    return message == null || message.isBlank() ? fallbackMessage : message;
  }

  private static boolean isTimeoutException(RpcException exception) {
    if (exception.isTimeout()) {
      return true;
    }
    String message = exception.getMessage();
    if (message != null) {
      String normalized = message.toLowerCase(Locale.ROOT);
      if (normalized.contains("timed out") || normalized.contains("timeout")) {
        return true;
      }
    }
    Throwable cause = exception.getCause();
    while (cause != null) {
      String causeMessage = cause.getMessage();
      if (causeMessage != null) {
        String normalizedCause = causeMessage.toLowerCase(Locale.ROOT);
        if (normalizedCause.contains("timed out") || normalizedCause.contains("timeout")) {
          return true;
        }
      }
      cause = cause.getCause();
    }
    return false;
  }
}
