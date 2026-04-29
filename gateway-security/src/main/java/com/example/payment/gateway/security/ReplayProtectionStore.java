package com.example.payment.gateway.security;

import java.time.Duration;

public interface ReplayProtectionStore {

  boolean recordIfAbsent(String merchantId, String nonce, Duration ttl);
}
