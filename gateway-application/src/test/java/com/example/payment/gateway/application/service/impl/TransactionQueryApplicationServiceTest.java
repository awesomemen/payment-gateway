package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.TransactionDetailRequest;
import com.example.payment.gateway.api.payment.TransactionDetailResponse;
import com.example.payment.gateway.common.payment.PaymentOrderRecord;
import com.example.payment.gateway.common.payment.PaymentOrderRepository;
import com.example.payment.gateway.common.payment.RefundOrderRecord;
import com.example.payment.gateway.common.payment.RefundOrderRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

class TransactionQueryApplicationServiceTest {

  private PaymentOrderRepository paymentOrderRepository;
  private RefundOrderRepository refundOrderRepository;
  private TransactionQueryApplicationServiceImpl service;

  @BeforeEach
  void setUp() {
    paymentOrderRepository = Mockito.mock(PaymentOrderRepository.class);
    refundOrderRepository = Mockito.mock(RefundOrderRepository.class);
    service = new TransactionQueryApplicationServiceImpl(paymentOrderRepository, refundOrderRepository);
  }

  @Test
  void shouldReturnPaymentTransaction() {
    given(paymentOrderRepository.findByGatewayPaymentId("GP123")).willReturn(Optional.of(new PaymentOrderRecord(
        "GP123", "MCH100001", "REQ-PAY-1", "IDEMP-1", "ROUTE_PAY_CREATE",
        "com.example.payment.api.PaymentCreateFacade", "DSP123", "SUCCEEDED", "88.50", "CNY"
    )));

    TransactionDetailResponse response = service.query(new TransactionDetailRequest(null, null, "GP123"));

    assertThat(response.bizType()).isEqualTo("PAY");
    assertThat(response.gatewayOrderId()).isEqualTo("GP123");
  }

  @Test
  void shouldReturnRefundTransactionByRequestId() {
    given(paymentOrderRepository.findByMerchantIdAndRequestId("MCH100001", "REQ-REFUND-1")).willReturn(Optional.empty());
    given(refundOrderRepository.findByMerchantIdAndRequestId("MCH100001", "REQ-REFUND-1")).willReturn(Optional.of(new RefundOrderRecord(
        "GR123", "MCH100001", "REQ-REFUND-1", "GP123", "RIDEMP-1", "ROUTE_REFUND_CREATE",
        "com.example.payment.api.RefundFacade", "DSR123", "PROCESSING", "10.50", "CNY"
    )));

    TransactionDetailResponse response = service.query(new TransactionDetailRequest("MCH100001", "REQ-REFUND-1", null));

    assertThat(response.bizType()).isEqualTo("REFUND");
    assertThat(response.gatewayOrderId()).isEqualTo("GR123");
  }
}
