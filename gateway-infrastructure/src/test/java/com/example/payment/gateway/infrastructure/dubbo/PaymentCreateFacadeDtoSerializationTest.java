package com.example.payment.gateway.infrastructure.dubbo;

import com.example.payment.api.PaymentCreateFacadeRequest;
import com.example.payment.api.PaymentCreateFacadeResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.hessian2.Hessian2Serialization;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentCreateFacadeDtoSerializationTest {

  private final Hessian2Serialization serialization = new Hessian2Serialization();

  @Test
  void shouldRoundTripFacadeRequestThroughHessian2() throws Exception {
    PaymentCreateFacadeRequest request = new PaymentCreateFacadeRequest(
        "MCH100001",
        "GP-20260422-0001",
        "REQ-20260422-0001",
        "IDEMP-20260422-0001",
        "88.5",
        "CNY",
        Instant.parse("2026-04-22T10:00:00Z"),
        "ROUTE_PAY_CREATE"
    );

    PaymentCreateFacadeRequest restored = roundTrip(request, PaymentCreateFacadeRequest.class);

    assertThat(restored.merchantId()).isEqualTo("MCH100001");
    assertThat(restored.gatewayPaymentId()).isEqualTo("GP-20260422-0001");
    assertThat(restored.requestId()).isEqualTo("REQ-20260422-0001");
    assertThat(restored.idempotencyKey()).isEqualTo("IDEMP-20260422-0001");
    assertThat(restored.amount()).isEqualTo("88.5");
    assertThat(restored.currency()).isEqualTo("CNY");
    assertThat(restored.requestTime()).isEqualTo(Instant.parse("2026-04-22T10:00:00Z"));
    assertThat(restored.routeCode()).isEqualTo("ROUTE_PAY_CREATE");
  }

  @Test
  void shouldRoundTripFacadeResponseThroughHessian2() throws Exception {
    PaymentCreateFacadeResponse response = new PaymentCreateFacadeResponse(
        "DSP-20260422-0001",
        "PROCESSING",
        "payment is processing in downstream facade"
    );

    PaymentCreateFacadeResponse restored = roundTrip(response, PaymentCreateFacadeResponse.class);

    assertThat(restored.downstreamPaymentId()).isEqualTo("DSP-20260422-0001");
    assertThat(restored.status()).isEqualTo("PROCESSING");
    assertThat(restored.message()).isEqualTo("payment is processing in downstream facade");
  }

  private <T> T roundTrip(T value, Class<T> type) throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ObjectOutput objectOutput = serialization.serialize(null, outputStream);
    objectOutput.writeObject(value);
    objectOutput.flushBuffer();

    ObjectInput objectInput = serialization.deserialize(null, new ByteArrayInputStream(outputStream.toByteArray()));
    return objectInput.readObject(type);
  }
}
