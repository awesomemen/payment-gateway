package com.example.payment.gateway.infrastructure.idempotency;

import com.example.payment.gateway.common.idempotency.PaymentIdempotencyRecord;
import com.example.payment.gateway.common.idempotency.PaymentIdempotencyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"local", "docker"})
public class RedisPaymentIdempotencyRepository implements PaymentIdempotencyRepository {

  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;

  public RedisPaymentIdempotencyRepository(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
    this.stringRedisTemplate = stringRedisTemplate;
    this.objectMapper = objectMapper;
  }

  @Override
  public Optional<PaymentIdempotencyRecord> find(String merchantId, String idempotencyKey) {
    String payload = stringRedisTemplate.opsForValue().get(cacheKey(merchantId, idempotencyKey));
    if (payload == null || payload.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(objectMapper.readValue(payload, PaymentIdempotencyRecord.class));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to deserialize idempotency record", exception);
    }
  }

  @Override
  public void save(PaymentIdempotencyRecord record, Duration ttl) {
    try {
      stringRedisTemplate.opsForValue().set(
          cacheKey(record.merchantId(), record.idempotencyKey()),
          objectMapper.writeValueAsString(record),
          ttl
      );
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize idempotency record", exception);
    }
  }

  private static String cacheKey(String merchantId, String idempotencyKey) {
    return "gateway:idempotency:" + merchantId + ":" + idempotencyKey;
  }
}
