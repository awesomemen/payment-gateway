package com.example.payment.gateway.infrastructure.dubbo;

import com.example.payment.api.RefundCreateFacadeRequest;
import com.example.payment.api.RefundCreateFacadeResponse;
import com.example.payment.api.RefundFacade;
import com.example.payment.gateway.common.exception.GatewayException;
import com.example.payment.gateway.common.payment.DownstreamRefundCreateGateway;
import com.example.payment.gateway.common.payment.DownstreamRefundCreateRequest;
import com.example.payment.gateway.common.payment.DownstreamRefundCreateResult;
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
public class DubboDownstreamRefundCreateGateway implements DownstreamRefundCreateGateway {

  private final RefundCreateFacadeMapper refundCreateFacadeMapper;

  @DubboReference(interfaceClass = RefundFacade.class, version = "1.0.0", check = false, timeout = 3000, retries = 0, injvm = false)
  private RefundFacade refundFacade;

  @Autowired
  public DubboDownstreamRefundCreateGateway(RefundCreateFacadeMapper refundCreateFacadeMapper) {
    this.refundCreateFacadeMapper = refundCreateFacadeMapper;
  }

  @Override
  public DownstreamRefundCreateResult create(
      GatewayRouteDefinition route,
      String gatewayRefundId,
      DownstreamRefundCreateRequest request
  ) {
    try {
      RefundCreateFacadeRequest facadeRequest = refundCreateFacadeMapper.toFacadeRequest(route, gatewayRefundId, request);
      RefundCreateFacadeResponse response = refundFacade.createRefund(facadeRequest);
      if (response == null) {
        throw new GatewayException(GatewayResponseCodes.DOWNSTREAM_EMPTY_RESPONSE, 502, "Downstream refund create facade returned empty response");
      }
      String normalizedStatus = normalizeStatus(response.status());
      return switch (normalizedStatus) {
        case "ACCEPTED", "PROCESSING" -> new DownstreamRefundCreateResult(response.downstreamRefundId(), normalizedStatus, response.message());
        case "REJECTED" -> throw new GatewayException(GatewayResponseCodes.DOWNSTREAM_REJECTED, 422, response.message());
        case "FAILED", "FAIL", "ERROR" -> throw new GatewayException(GatewayResponseCodes.DOWNSTREAM_FAILED, 502, response.message());
        default -> throw new GatewayException(GatewayResponseCodes.DOWNSTREAM_SERVICE_ERROR, 502, "Unsupported downstream refund status: " + response.status());
      };
    } catch (RpcException exception) {
      if (exception.isTimeout()) {
        throw new GatewayException(GatewayResponseCodes.DOWNSTREAM_TIMEOUT, 504, "Downstream refund create timed out");
      }
      throw new GatewayException(GatewayResponseCodes.DOWNSTREAM_SERVICE_ERROR, 502, "Downstream refund create invocation failed");
    }
  }

  private static String normalizeStatus(String status) {
    if (status == null || status.isBlank()) {
      throw new GatewayException(GatewayResponseCodes.DOWNSTREAM_EMPTY_RESPONSE, 502, "Downstream refund create facade returned empty status");
    }
    return status.trim().toUpperCase(Locale.ROOT);
  }
}
