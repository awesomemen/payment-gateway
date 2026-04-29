package com.example.payment.gateway.security;

import com.example.payment.gateway.common.exception.GatewayException;
import com.example.payment.gateway.common.response.GatewayResponseCodes;
import com.example.payment.gateway.domain.model.Money;
import com.example.payment.gateway.domain.model.PaymentCreateCommand;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultPaymentRequestSecurityValidatorTest {

  private static final Instant NOW = Instant.parse("2026-04-21T10:00:00Z");
  private static final String MERCHANT_ID = "MCH100001";
  private static final String SIGNATURE_KEY = "demo-signature-key";

  private DefaultPaymentRequestSecurityValidator validator;

  @BeforeEach
  void setUp() {
    GatewaySecurityProperties properties = new GatewaySecurityProperties();
    properties.setConfigSource("test-properties");
    properties.setRequestExpireSeconds(300);
    properties.setReplayProtectSeconds(300);
    properties.getMerchants().put(MERCHANT_ID, new GatewaySecurityProperties.MerchantProperties(true, SIGNATURE_KEY));

    validator = new DefaultPaymentRequestSecurityValidator(
        properties,
        new PropertiesMerchantCredentialProvider(properties),
        new InMemoryReplayProtectionStore(Clock.fixed(NOW, ZoneOffset.UTC)),
        Clock.fixed(NOW, ZoneOffset.UTC)
    );
  }

  @Test
  void shouldRejectUnknownMerchant() {
    PaymentCreateCommand command = validCommand("MCH404", "nonce-001", NOW.minusSeconds(30));

    assertThatThrownBy(() -> validator.validate(command))
        .isInstanceOf(GatewayException.class)
        .extracting("code")
        .isEqualTo(GatewayResponseCodes.MERCHANT_NOT_FOUND);
  }

  @Test
  void shouldRejectInvalidSignature() {
    PaymentCreateCommand command = new PaymentCreateCommand(
        MERCHANT_ID,
        "REQ-20260421-0001",
        "IDEMP-20260421-0001",
        new Money(new BigDecimal("88.50"), "CNY"),
        NOW.minusSeconds(30),
        "nonce-001",
        "invalid-signature"
    );

    assertThatThrownBy(() -> validator.validate(command))
        .isInstanceOf(GatewayException.class)
        .extracting("code")
        .isEqualTo(GatewayResponseCodes.SIGNATURE_INVALID);
  }

  @Test
  void shouldRejectExpiredRequest() {
    PaymentCreateCommand command = validCommand(MERCHANT_ID, "nonce-001", NOW.minus(Duration.ofMinutes(6)));

    assertThatThrownBy(() -> validator.validate(command))
        .isInstanceOf(GatewayException.class)
        .extracting("code")
        .isEqualTo(GatewayResponseCodes.REQUEST_EXPIRED);
  }

  @Test
  void shouldRejectReplayedNonce() {
    PaymentCreateCommand firstCommand = validCommand(MERCHANT_ID, "nonce-001", NOW.minusSeconds(30));
    PaymentCreateCommand replayedCommand = validCommand(MERCHANT_ID, "nonce-001", NOW.minusSeconds(20));

    assertThatCode(() -> validator.validate(firstCommand))
        .doesNotThrowAnyException();

    assertThatThrownBy(() -> validator.validate(replayedCommand))
        .isInstanceOf(GatewayException.class)
        .extracting("code")
        .isEqualTo(GatewayResponseCodes.REQUEST_REPLAYED);
  }

  @Test
  void shouldAcceptValidRequest() {
    PaymentCreateCommand command = validCommand(MERCHANT_ID, "nonce-001", NOW.minusSeconds(30));

    assertThatCode(() -> validator.validate(command))
        .doesNotThrowAnyException();
  }

  private static PaymentCreateCommand validCommand(String merchantId, String nonce, Instant requestTime) {
    Money amount = new Money(new BigDecimal("88.50"), "CNY");
    return new PaymentCreateCommand(
        merchantId,
        "REQ-20260421-0001",
        "IDEMP-20260421-0001",
        amount,
        requestTime,
        nonce,
        sign(merchantId, "REQ-20260421-0001", "IDEMP-20260421-0001", amount, requestTime, nonce, SIGNATURE_KEY)
    );
  }

  private static String sign(
      String merchantId,
      String requestId,
      String idempotencyKey,
      Money amount,
      Instant requestTime,
      String nonce,
      String signatureKey
  ) {
    String payload = String.join("&",
        "merchantId=" + merchantId,
        "requestId=" + requestId,
        "idempotencyKey=" + idempotencyKey,
        "amount=" + amount.amount().stripTrailingZeros().toPlainString(),
        "currency=" + amount.currency(),
        "requestTime=" + requestTime,
        "nonce=" + nonce
    );
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(signatureKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to sign payload for test", exception);
    }
  }
}
