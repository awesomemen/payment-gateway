package com.example.payment.gateway.infrastructure.payment;

import com.example.payment.gateway.security.ReplayProtectionStore;
import java.time.Duration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "docker"})
public class RedisReplayProtectionStore implements ReplayProtectionStore {

  private final StringRedisTemplate stringRedisTemplate;

  public RedisReplayProtectionStore(StringRedisTemplate stringRedisTemplate) {
    this.stringRedisTemplate = stringRedisTemplate;
  }

  @Override
  public boolean recordIfAbsent(String merchantId, String nonce, Duration ttl) {
    Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(
        "gateway:replay:" + merchantId + ":" + nonce,
        "1",
        ttl
    );
    return Boolean.TRUE.equals(result);
  }
}
