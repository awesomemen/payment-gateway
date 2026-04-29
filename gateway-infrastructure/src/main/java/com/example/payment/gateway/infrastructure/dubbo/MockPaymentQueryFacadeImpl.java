package com.example.payment.gateway.infrastructure.dubbo;

import com.example.payment.api.PaymentQueryFacade;
import com.example.payment.api.PaymentQueryFacadeRequest;
import com.example.payment.api.PaymentQueryFacadeResponse;
import java.util.Locale;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;

@DubboService(interfaceClass = PaymentQueryFacade.class, version = "1.0.0")
@Profile({"local", "docker"})
@ConditionalOnProperty(prefix = "gateway.downstream.sandbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MockPaymentQueryFacadeImpl implements PaymentQueryFacade {

  private final MockDownstreamPaymentStore paymentStore;

  public MockPaymentQueryFacadeImpl(MockDownstreamPaymentStore paymentStore) {
    this.paymentStore = paymentStore;
  }

  @Override
  public PaymentQueryFacadeResponse queryPayment(PaymentQueryFacadeRequest request) {
    String requestId = request.requestId();
    if (requestId != null && requestId.startsWith("REQ-TIMEOUT")) {
      throw new RpcException("payment query timed out in mock downstream facade");
    }
    if (requestId != null && requestId.startsWith("REQ-ERROR")) {
      throw new RpcException("payment query failed in mock downstream facade");
    }
    return paymentStore.find(request)
        .map(MockPaymentQueryFacadeImpl::mapStoredContractResponse)
        .orElseGet(() -> fallbackResponse(requestId));
  }

  private static PaymentQueryFacadeResponse mapStoredContractResponse(MockDownstreamPaymentStore.StoredPaymentContract contract) {
    String normalizedStatus = contract.createStatus().trim().toUpperCase(Locale.ROOT);
    return switch (normalizedStatus) {
      case "ACCEPTED" -> new PaymentQueryFacadeResponse("SUCCEEDED", "payment succeeded in mock downstream facade");
      case "PROCESSING" -> new PaymentQueryFacadeResponse("PROCESSING", "payment is still processing in mock downstream facade");
      case "REJECTED" -> new PaymentQueryFacadeResponse("CLOSED", "payment closed in mock downstream facade");
      case "FAILED", "SUCCEEDED", "CLOSED" -> new PaymentQueryFacadeResponse(normalizedStatus, contract.createMessage());
      default -> new PaymentQueryFacadeResponse("SUCCEEDED", "payment succeeded in mock downstream facade");
    };
  }

  private static PaymentQueryFacadeResponse fallbackResponse(String requestId) {
    if (requestId != null && requestId.startsWith("REQ-PROCESSING")) {
      return new PaymentQueryFacadeResponse("PROCESSING", "payment is still processing in mock downstream facade");
    }
    if (requestId != null && requestId.startsWith("REQ-CLOSE")) {
      return new PaymentQueryFacadeResponse("CLOSED", "payment closed in mock downstream facade");
    }
    return new PaymentQueryFacadeResponse("SUCCEEDED", "payment succeeded in mock downstream facade");
  }
}
