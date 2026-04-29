package com.example.payment.gateway.infrastructure.dubbo;

import com.example.payment.api.PaymentQueryFacade;
import com.example.payment.api.PaymentQueryFacadeResponse;
import com.example.payment.gateway.common.exception.GatewayException;
import com.example.payment.gateway.common.payment.DownstreamPaymentQueryGateway;
import com.example.payment.gateway.common.payment.DownstreamPaymentQueryRequest;
import com.example.payment.gateway.common.payment.DownstreamPaymentQueryResult;
import com.example.payment.gateway.common.payment.GatewayRouteDefinition;
import com.example.payment.gateway.common.response.GatewayResponseCodes;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "docker"})
public class DubboDownstreamPaymentQueryGateway implements DownstreamPaymentQueryGateway {

  private final PaymentQueryFacadeMapper paymentQueryFacadeMapper;

  @DubboReference(interfaceClass = PaymentQueryFacade.class, version = "1.0.0", check = false, timeout = 2000, retries = 0, injvm = false)
  private PaymentQueryFacade paymentQueryFacade;

  @Autowired
  public DubboDownstreamPaymentQueryGateway(PaymentQueryFacadeMapper paymentQueryFacadeMapper) {
    this.paymentQueryFacadeMapper = paymentQueryFacadeMapper;
  }

  DubboDownstreamPaymentQueryGateway(
      PaymentQueryFacade paymentQueryFacade,
      PaymentQueryFacadeMapper paymentQueryFacadeMapper
  ) {
    this.paymentQueryFacade = paymentQueryFacade;
    this.paymentQueryFacadeMapper = paymentQueryFacadeMapper;
  }

  @Override
  public DownstreamPaymentQueryResult query(GatewayRouteDefinition route, DownstreamPaymentQueryRequest request) {
    try {
      PaymentQueryFacadeResponse response = paymentQueryFacade.queryPayment(
          paymentQueryFacadeMapper.toFacadeRequest(request)
      );
      if (response == null || response.status() == null || response.status().isBlank()) {
        throw new GatewayException(
            GatewayResponseCodes.DOWNSTREAM_EMPTY_RESPONSE,
            502,
            "Downstream payment query returned empty status"
        );
      }
      return new DownstreamPaymentQueryResult(response.status(), response.message());
    } catch (GatewayException exception) {
      throw exception;
    } catch (RpcException exception) {
      if (isTimeoutException(exception)) {
        throw new GatewayException(
            GatewayResponseCodes.DOWNSTREAM_TIMEOUT,
            504,
            "Downstream payment query timed out"
        );
      }
      throw new GatewayException(
          GatewayResponseCodes.DOWNSTREAM_SERVICE_ERROR,
          502,
          "Downstream payment query invocation failed"
      );
    } catch (Exception exception) {
      throw new GatewayException(
          GatewayResponseCodes.DOWNSTREAM_SERVICE_ERROR,
          502,
          "Downstream payment query invocation failed"
      );
    }
  }

  private static boolean isTimeoutException(RpcException exception) {
    if (exception.isTimeout()) {
      return true;
    }
    Throwable current = exception;
    while (current != null) {
      String message = current.getMessage();
      if (message != null) {
        String normalized = message.toLowerCase();
        if (normalized.contains("timed out") || normalized.contains("timeout")) {
          return true;
        }
      }
      current = current.getCause();
    }
    return false;
  }
}
