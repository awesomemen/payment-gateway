package com.example.payment.gateway.infrastructure.dubbo;

import com.example.payment.api.RefundCreateFacadeRequest;
import com.example.payment.api.RefundCreateFacadeResponse;
import com.example.payment.api.RefundFacade;
import com.example.payment.api.RefundQueryFacadeRequest;
import com.example.payment.api.RefundQueryFacadeResponse;
import java.util.Locale;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;

@DubboService(interfaceClass = RefundFacade.class, version = "1.0.0")
@Profile({"local", "docker"})
@ConditionalOnProperty(prefix = "gateway.downstream.sandbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MockRefundFacadeImpl implements RefundFacade {

  private final MockDownstreamRefundStore refundStore;

  public MockRefundFacadeImpl(MockDownstreamRefundStore refundStore) {
    this.refundStore = refundStore;
  }

  @Override
  public RefundCreateFacadeResponse createRefund(RefundCreateFacadeRequest request) {
    String requestId = request.requestId();
    if (requestId != null && requestId.startsWith("REQ-REFUND-TIMEOUT")) {
      throw new RpcException("refund create timed out in mock downstream facade");
    }
    if (requestId != null && requestId.startsWith("REQ-REFUND-ERROR")) {
      throw new RpcException("refund create failed in mock downstream facade");
    }
    if (requestId != null && requestId.startsWith("REQ-REFUND-PROCESSING")) {
      return persistAndRespond(request, "PROCESSING", "refund is processing in mock downstream facade");
    }
    return persistAndRespond(request, "ACCEPTED", "refund accepted by mock downstream facade");
  }

  @Override
  public RefundQueryFacadeResponse queryRefund(RefundQueryFacadeRequest request) {
    String requestId = request.requestId();
    if (requestId != null && requestId.startsWith("REQ-REFUND-QUERY-TIMEOUT")) {
      throw new RpcException("refund query timed out in mock downstream facade");
    }
    if (requestId != null && requestId.startsWith("REQ-REFUND-QUERY-ERROR")) {
      throw new RpcException("refund query failed in mock downstream facade");
    }
    return refundStore.find(request)
        .map(MockRefundFacadeImpl::mapStoredContractResponse)
        .orElseGet(() -> fallbackResponse(requestId));
  }

  private RefundCreateFacadeResponse persistAndRespond(
      RefundCreateFacadeRequest request,
      String status,
      String message
  ) {
    String downstreamRefundId = "DSR" + Integer.toUnsignedString((request.merchantId() + ":" + request.requestId()).hashCode());
    refundStore.saveCreateResult(request, downstreamRefundId, status, message);
    return new RefundCreateFacadeResponse(downstreamRefundId, status, message);
  }

  private static RefundQueryFacadeResponse mapStoredContractResponse(MockDownstreamRefundStore.StoredRefundContract contract) {
    String normalizedStatus = contract.createStatus().trim().toUpperCase(Locale.ROOT);
    return switch (normalizedStatus) {
      case "ACCEPTED" -> new RefundQueryFacadeResponse("SUCCEEDED", "refund succeeded in mock downstream facade");
      case "PROCESSING" -> new RefundQueryFacadeResponse("PROCESSING", "refund is still processing in mock downstream facade");
      case "FAILED", "SUCCEEDED", "CLOSED" -> new RefundQueryFacadeResponse(normalizedStatus, contract.createMessage());
      default -> new RefundQueryFacadeResponse("SUCCEEDED", "refund succeeded in mock downstream facade");
    };
  }

  private static RefundQueryFacadeResponse fallbackResponse(String requestId) {
    if (requestId != null && requestId.startsWith("REQ-REFUND-PROCESSING")) {
      return new RefundQueryFacadeResponse("PROCESSING", "refund is still processing in mock downstream facade");
    }
    return new RefundQueryFacadeResponse("SUCCEEDED", "refund succeeded in mock downstream facade");
  }
}
