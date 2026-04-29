package com.example.payment.gateway.infrastructure.dubbo;

import com.example.payment.api.PaymentCreateFacadeRequest;
import com.example.payment.api.PaymentQueryFacadeRequest;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "docker"})
@ConditionalOnProperty(prefix = "gateway.downstream.sandbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MockDownstreamPaymentStore {

  private final Map<String, StoredPaymentContract> paymentsByRequestId = new ConcurrentHashMap<>();
  private final Map<String, StoredPaymentContract> paymentsByGatewayPaymentId = new ConcurrentHashMap<>();
  private final Map<String, StoredPaymentContract> paymentsByDownstreamPaymentId = new ConcurrentHashMap<>();

  public void saveCreateResult(
      PaymentCreateFacadeRequest request,
      String downstreamPaymentId,
      String status,
      String message
  ) {
    StoredPaymentContract contract = new StoredPaymentContract(
        request.merchantId(),
        request.requestId(),
        request.gatewayPaymentId(),
        downstreamPaymentId,
        status,
        message
    );
    paymentsByRequestId.put(contract.requestId(), contract);
    paymentsByGatewayPaymentId.put(contract.gatewayPaymentId(), contract);
    paymentsByDownstreamPaymentId.put(contract.downstreamPaymentId(), contract);
  }

  public Optional<StoredPaymentContract> find(PaymentQueryFacadeRequest request) {
    return Optional.ofNullable(paymentsByDownstreamPaymentId.get(request.downstreamPaymentId()))
        .or(() -> Optional.ofNullable(paymentsByGatewayPaymentId.get(request.gatewayPaymentId())))
        .or(() -> Optional.ofNullable(paymentsByRequestId.get(request.requestId())));
  }

  public record StoredPaymentContract(
      String merchantId,
      String requestId,
      String gatewayPaymentId,
      String downstreamPaymentId,
      String createStatus,
      String createMessage
  ) {
  }
}
