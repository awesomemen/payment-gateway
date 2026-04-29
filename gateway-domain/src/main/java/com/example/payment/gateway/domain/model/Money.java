package com.example.payment.gateway.domain.model;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

public record Money(BigDecimal amount, String currency) {

  public Money {
    Objects.requireNonNull(amount, "amount must not be null");
    Objects.requireNonNull(currency, "currency must not be null");
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("amount must be positive");
    }
    currency = currency.trim().toUpperCase(Locale.ROOT);
    if (currency.isBlank()) {
      throw new IllegalArgumentException("currency must not be blank");
    }
  }
}
