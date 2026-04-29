package com.example.payment.gateway.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("default")
public class InMemoryReplayProtectionStore implements ReplayProtectionStore {

  private final ConcurrentHashMap<String, Instant> expiresAtByKey = new ConcurrentHashMap<>();
  private final Clock clock;

  public InMemoryReplayProtectionStore(Clock clock) {
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  @Override
  public boolean recordIfAbsent(String merchantId, String nonce, Duration ttl) {
    Objects.requireNonNull(ttl, "ttl must not be null");
    Instant now = Instant.now(clock);
    Instant expiresAt = now.plus(ttl);
    String key = merchantId + ":" + nonce;
    AtomicBoolean recorded = new AtomicBoolean(false);

    expiresAtByKey.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
    expiresAtByKey.compute(key, (ignored, existingExpiresAt) -> {
      if (existingExpiresAt != null && existingExpiresAt.isAfter(now)) {
        return existingExpiresAt;
      }
      recorded.set(true);
      return expiresAt;
    });
    return recorded.get();
  }
}
