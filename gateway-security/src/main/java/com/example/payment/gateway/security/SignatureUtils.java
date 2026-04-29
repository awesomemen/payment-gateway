package com.example.payment.gateway.security;

import com.example.payment.gateway.domain.model.SignedPaymentCommand;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class SignatureUtils {

  private SignatureUtils() {
  }

  static String hmacSha256Hex(SignedPaymentCommand command, String signatureKey) {
    String payload = command.signaturePayload();
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(signatureKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("Unable to calculate signature", exception);
    }
  }
}
