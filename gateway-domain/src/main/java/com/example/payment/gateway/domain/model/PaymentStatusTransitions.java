package com.example.payment.gateway.domain.model;

import java.util.Locale;
import java.util.Set;

public final class PaymentStatusTransitions {

  private static final Set<String> FINAL_STATUSES = Set.of("SUCCEEDED", "FAILED", "CLOSED");
  private static final Set<String> ACTIVE_STATUSES = Set.of("ACCEPTED", "PROCESSING");

  private PaymentStatusTransitions() {
  }

  public static String normalize(String status) {
    if (status == null || status.isBlank()) {
      throw new IllegalArgumentException("payment status must not be blank");
    }
    return status.trim().toUpperCase(Locale.ROOT);
  }

  public static boolean isFinal(String status) {
    return FINAL_STATUSES.contains(normalize(status));
  }

  public static boolean isTransitionAllowed(String currentStatus, String targetStatus) {
    String normalizedCurrent = normalize(currentStatus);
    String normalizedTarget = normalize(targetStatus);
    if (normalizedCurrent.equals(normalizedTarget)) {
      return true;
    }
    if (ACTIVE_STATUSES.contains(normalizedCurrent)) {
      return ACTIVE_STATUSES.contains(normalizedTarget) || FINAL_STATUSES.contains(normalizedTarget);
    }
    return false;
  }
}
