package com.example.payment.gateway.infrastructure.dubbo;

import com.example.payment.api.RefundCreateFacadeRequest;
import com.example.payment.api.RefundQueryFacadeRequest;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "docker"})
@ConditionalOnProperty(prefix = "gateway.downstream.sandbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MockDownstreamRefundStore {

  private final Map<String, StoredRefundContract> refundsByRequestId = new ConcurrentHashMap<>();
  private final Map<String, StoredRefundContract> refundsByGatewayRefundId = new ConcurrentHashMap<>();
  private final Map<String, StoredRefundContract> refundsByDownstreamRefundId = new ConcurrentHashMap<>();

  public void saveCreateResult(
      RefundCreateFacadeRequest request,
      String downstreamRefundId,
      String status,
      String message
  ) {
    StoredRefundContract contract = new StoredRefundContract(
        request.merchantId(),
        request.requestId(),
        request.gatewayRefundId(),
        downstreamRefundId,
        status,
        message
    );
    refundsByRequestId.put(contract.requestId(), contract);
    refundsByGatewayRefundId.put(contract.gatewayRefundId(), contract);
    refundsByDownstreamRefundId.put(contract.downstreamRefundId(), contract);
  }

  public Optional<StoredRefundContract> find(RefundQueryFacadeRequest request) {
    return Optional.ofNullable(refundsByDownstreamRefundId.get(request.downstreamRefundId()))
        .or(() -> Optional.ofNullable(refundsByGatewayRefundId.get(request.gatewayRefundId())))
        .or(() -> Optional.ofNullable(refundsByRequestId.get(request.requestId())));
  }

  public record StoredRefundContract(
      String merchantId,
      String requestId,
      String gatewayRefundId,
      String downstreamRefundId,
      String createStatus,
      String createMessage
  ) {
  }
}
