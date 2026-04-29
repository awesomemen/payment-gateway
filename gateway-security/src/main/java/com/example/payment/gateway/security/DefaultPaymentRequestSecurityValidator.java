package com.example.payment.gateway.security;

import com.example.payment.gateway.common.exception.GatewayException;
import com.example.payment.gateway.common.response.GatewayResponseCodes;
import com.example.payment.gateway.domain.model.SignedPaymentCommand;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class DefaultPaymentRequestSecurityValidator implements PaymentRequestSecurityValidator {

  private final GatewaySecurityProperties properties;
  private final MerchantCredentialProvider merchantCredentialProvider;
  private final ReplayProtectionStore replayProtectionStore;
  private final Clock clock;

  public DefaultPaymentRequestSecurityValidator(
      GatewaySecurityProperties properties,
      MerchantCredentialProvider merchantCredentialProvider,
      ReplayProtectionStore replayProtectionStore,
      Clock clock
  ) {
    this.properties = Objects.requireNonNull(properties, "properties must not be null");
    this.merchantCredentialProvider = Objects.requireNonNull(merchantCredentialProvider, "merchantCredentialProvider must not be null");
    this.replayProtectionStore = Objects.requireNonNull(replayProtectionStore, "replayProtectionStore must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  @Override
  public void validate(SignedPaymentCommand command, boolean checkReplay) {
    MerchantCredential merchant = merchantCredentialProvider.find(command.merchantId())
        .filter(MerchantCredential::enabled)
        .orElse(null);
    if (merchant == null) {
      throw new GatewayException(GatewayResponseCodes.MERCHANT_NOT_FOUND, 401, "Merchant credentials not found");
    }

    validateRequestTime(command.requestTime());
    validateSignature(command, merchant.signatureKey());
    if (checkReplay) {
      validateReplay(command);
    }
  }

  private void validateRequestTime(Instant requestTime) {
    Duration allowedWindow = Duration.ofSeconds(properties.getRequestExpireSeconds());
    Duration skew = Duration.between(requestTime, Instant.now(clock)).abs();
    if (skew.compareTo(allowedWindow) > 0) {
      throw new GatewayException(GatewayResponseCodes.REQUEST_EXPIRED, 400, "Request timestamp is outside the accepted window");
    }
  }

  private void validateSignature(SignedPaymentCommand command, String signatureKey) {
    String expected = SignatureUtils.hmacSha256Hex(command, signatureKey);
    boolean matched = MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8),
        command.signature().getBytes(StandardCharsets.UTF_8)
    );
    if (!matched) {
      throw new GatewayException(GatewayResponseCodes.SIGNATURE_INVALID, 401, "Invalid request signature");
    }
  }

  private void validateReplay(SignedPaymentCommand command) {
    boolean recorded = replayProtectionStore.recordIfAbsent(
        command.merchantId(),
        command.nonce(),
        Duration.ofSeconds(properties.getReplayProtectSeconds())
    );
    if (!recorded) {
      throw new GatewayException(GatewayResponseCodes.REQUEST_REPLAYED, 401, "Replay attack detected");
    }
  }
}
