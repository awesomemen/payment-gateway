package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.TransactionDetailRequest;
import com.example.payment.gateway.api.payment.TransactionDetailResponse;
import com.example.payment.gateway.application.service.TransactionQueryApplicationService;
import com.example.payment.gateway.common.exception.GatewayException;
import com.example.payment.gateway.common.payment.PaymentBizTypes;
import com.example.payment.gateway.common.payment.PaymentOrderRecord;
import com.example.payment.gateway.common.payment.PaymentOrderRepository;
import com.example.payment.gateway.common.payment.RefundOrderRecord;
import com.example.payment.gateway.common.payment.RefundOrderRepository;
import com.example.payment.gateway.common.response.GatewayResponseCodes;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class TransactionQueryApplicationServiceImpl implements TransactionQueryApplicationService {

  private final PaymentOrderRepository paymentOrderRepository;
  private final RefundOrderRepository refundOrderRepository;

  public TransactionQueryApplicationServiceImpl(
      PaymentOrderRepository paymentOrderRepository,
      RefundOrderRepository refundOrderRepository
  ) {
    this.paymentOrderRepository = paymentOrderRepository;
    this.refundOrderRepository = refundOrderRepository;
  }

  @Override
  public TransactionDetailResponse query(TransactionDetailRequest request) {
    Optional<PaymentOrderRecord> paymentOrder = findPayment(request);
    if (paymentOrder.isPresent()) {
      PaymentOrderRecord record = paymentOrder.get();
      return new TransactionDetailResponse(
          PaymentBizTypes.PAY,
          record.requestId(),
          record.gatewayPaymentId(),
          record.downstreamPaymentId(),
          record.paymentStatus(),
          record.amount(),
          record.currency(),
          record.routeCode(),
          record.targetService()
      );
    }
    Optional<RefundOrderRecord> refundOrder = findRefund(request);
    if (refundOrder.isPresent()) {
      RefundOrderRecord record = refundOrder.get();
      return new TransactionDetailResponse(
          PaymentBizTypes.REFUND,
          record.requestId(),
          record.gatewayRefundId(),
          record.downstreamRefundId(),
          record.refundStatus(),
          record.amount(),
          record.currency(),
          record.routeCode(),
          record.targetService()
      );
    }
    throw new GatewayException(GatewayResponseCodes.PAYMENT_ORDER_NOT_FOUND, 404, "Transaction not found");
  }

  private Optional<PaymentOrderRecord> findPayment(TransactionDetailRequest request) {
    if (request.gatewayOrderId() != null && !request.gatewayOrderId().isBlank() && request.gatewayOrderId().startsWith("GP")) {
      return paymentOrderRepository.findByGatewayPaymentId(request.gatewayOrderId());
    }
    if (request.merchantId() != null && request.requestId() != null) {
      return paymentOrderRepository.findByMerchantIdAndRequestId(request.merchantId(), request.requestId());
    }
    return Optional.empty();
  }

  private Optional<RefundOrderRecord> findRefund(TransactionDetailRequest request) {
    if (request.gatewayOrderId() != null && !request.gatewayOrderId().isBlank() && request.gatewayOrderId().startsWith("GR")) {
      return refundOrderRepository.findByGatewayRefundId(request.gatewayOrderId());
    }
    if (request.merchantId() != null && request.requestId() != null) {
      return refundOrderRepository.findByMerchantIdAndRequestId(request.merchantId(), request.requestId());
    }
    return Optional.empty();
  }
}
