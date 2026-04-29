package com.example.payment.gateway.governance.payment;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.payment.gateway.common.exception.GatewayException;
import com.example.payment.gateway.common.response.GatewayResponseCodes;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile({"local", "docker"})
public class RedisPaymentGovernanceGuard implements PaymentGovernanceGuard {

  public static final String PAYMENT_CREATE_RESOURCE = "gateway:pay:create";

  private static final DateTimeFormatter WINDOW_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
      .withZone(ZoneOffset.UTC);

  private final StringRedisTemplate stringRedisTemplate;
  private final GatewayGovernanceProperties properties;

  public RedisPaymentGovernanceGuard(StringRedisTemplate stringRedisTemplate, GatewayGovernanceProperties properties) {
    this.stringRedisTemplate = stringRedisTemplate;
    this.properties = properties;
  }

  @Override
  public void guardPaymentCreate(String merchantId) {
    try (Entry ignored = SphU.entry(PAYMENT_CREATE_RESOURCE)) {
      int permitsPerMinute = properties.getPermitsPerMinute();
      if (permitsPerMinute <= 0) {
        return;
      }
      String key = "gateway:rate:" + PAYMENT_CREATE_RESOURCE + ":" + merchantId + ":" + WINDOW_FORMATTER.format(Instant.now());
      Long current = stringRedisTemplate.opsForValue().increment(key);
      if (current != null && current == 1L) {
        stringRedisTemplate.expire(key, Duration.ofMinutes(2));
      }
      if (current != null && current > permitsPerMinute) {
        throw new GatewayException(
            GatewayResponseCodes.RATE_LIMITED,
            429,
            "Payment create request has been rate limited"
        );
      }
    } catch (BlockException exception) {
      throw new GatewayException(
          GatewayResponseCodes.RATE_LIMITED,
          429,
          "Payment create request has been blocked by Sentinel"
      );
    }
  }

  @Override
  public int currentPermitsPerMinute() {
    return properties.getPermitsPerMinute();
  }
}
