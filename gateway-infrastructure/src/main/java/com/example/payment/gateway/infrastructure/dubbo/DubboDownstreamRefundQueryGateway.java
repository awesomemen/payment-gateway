package com.example.payment.gateway.infrastructure.dubbo;

import com.example.payment.api.RefundFacade;
import com.example.payment.api.RefundQueryFacadeResponse;
import com.example.payment.gateway.common.exception.GatewayException;
import com.example.payment.gateway.common.payment.DownstreamRefundQueryGateway;
import com.example.payment.gateway.common.payment.DownstreamRefundQueryRequest;
import com.example.payment.gateway.common.payment.DownstreamRefundQueryResult;
import com.example.payment.gateway.common.payment.GatewayRouteDefinition;
import com.example.payment.gateway.common.response.GatewayResponseCodes;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "docker"})
public class DubboDownstreamRefundQueryGateway implements DownstreamRefundQueryGateway {

  private final RefundQueryFacadeMapper refundQueryFacadeMapper;

  @DubboReference(interfaceClass = RefundFacade.class, version = "1.0.0", check = false, timeout = 3000, retries = 0, injvm = false)
  private RefundFacade refundFacade;

  @Autowired
  public DubboDownstreamRefundQueryGateway(RefundQueryFacadeMapper refundQueryFacadeMapper) {
    this.refundQueryFacadeMapper = refundQueryFacadeMapper;
  }

  @Override
  public DownstreamRefundQueryResult query(GatewayRouteDefinition route, DownstreamRefundQueryRequest request) {
    try {
      RefundQueryFacadeResponse response = refundFacade.queryRefund(refundQueryFacadeMapper.toFacadeRequest(route, request));
      if (response == null || response.status() == null || response.status().isBlank()) {
        throw new GatewayException(GatewayResponseCodes.DOWNSTREAM_EMPTY_RESPONSE, 502, "Downstream refund query returned empty status");
      }
      return new DownstreamRefundQueryResult(response.status(), response.message());
    } catch (RpcException exception) {
      if (exception.isTimeout()) {
        throw new GatewayException(GatewayResponseCodes.DOWNSTREAM_TIMEOUT, 504, "Downstream refund query timed out");
      }
      throw new GatewayException(GatewayResponseCodes.DOWNSTREAM_SERVICE_ERROR, 502, "Downstream refund query invocation failed");
    }
  }
}
