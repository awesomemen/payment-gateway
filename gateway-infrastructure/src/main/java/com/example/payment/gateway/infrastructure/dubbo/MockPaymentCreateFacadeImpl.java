package com.example.payment.gateway.infrastructure.dubbo;

import com.example.payment.api.PaymentCreateFacade;
import com.example.payment.api.PaymentCreateFacadeRequest;
import com.example.payment.api.PaymentCreateFacadeResponse;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;

@DubboService(interfaceClass = PaymentCreateFacade.class, version = "1.0.0")
@Profile({"local", "docker"})
@ConditionalOnProperty(prefix = "gateway.downstream.sandbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MockPaymentCreateFacadeImpl implements PaymentCreateFacade {

  private final MockDownstreamPaymentStore paymentStore;

  public MockPaymentCreateFacadeImpl(MockDownstreamPaymentStore paymentStore) {
    this.paymentStore = paymentStore;
  }

  @Override
  public PaymentCreateFacadeResponse createPayment(PaymentCreateFacadeRequest request) {
    String requestId = request.requestId();
    if (requestId != null && requestId.startsWith("REQ-TIMEOUT")) {
      throw new RpcException("payment create timed out in mock downstream facade");
    }
    if (requestId != null && requestId.startsWith("REQ-ERROR")) {
      throw new RpcException("payment create failed in mock downstream facade");
    }
    if (requestId != null && requestId.startsWith("REQ-REJECT")) {
      return persistAndRespond(request, "REJECTED", "payment rejected by mock downstream facade");
    }
    if (requestId != null && requestId.startsWith("REQ-FAIL")) {
      return persistAndRespond(request, "FAILED", "payment failed in mock downstream facade");
    }
    if (requestId != null && requestId.startsWith("REQ-PROCESSING")) {
      return persistAndRespond(request, "PROCESSING", "payment is processing in mock downstream facade");
    }
    return persistAndRespond(request, "ACCEPTED", "Payment request accepted by mock downstream facade");
  }

  private PaymentCreateFacadeResponse persistAndRespond(
      PaymentCreateFacadeRequest request,
      String status,
      String message
  ) {
    String downstreamPaymentId = "DSP" + Integer.toUnsignedString((request.merchantId() + ":" + request.requestId()).hashCode());
    paymentStore.saveCreateResult(request, downstreamPaymentId, status, message);
    return new PaymentCreateFacadeResponse(
        downstreamPaymentId,
        status,
        message
    );
  }
}
