package com.example.payment.gateway.infrastructure.idempotency;

import com.example.payment.gateway.common.idempotency.PaymentIdempotencyRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class RedisPaymentIdempotencyRepositoryTest {

  @Container
  static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.2.13").withExposedPorts(6379);

  private LettuceConnectionFactory connectionFactory;

  @AfterEach
  void tearDown() {
    if (connectionFactory != null) {
      connectionFactory.destroy();
    }
  }

  @Test
  void shouldSaveAndLoadIdempotencyRecord() {
    connectionFactory = new LettuceConnectionFactory(new RedisStandaloneConfiguration(
        REDIS.getHost(),
        REDIS.getMappedPort(6379)
    ));
    connectionFactory.afterPropertiesSet();

    StringRedisTemplate stringRedisTemplate = new StringRedisTemplate(connectionFactory);
    stringRedisTemplate.afterPropertiesSet();

    RedisPaymentIdempotencyRepository repository = new RedisPaymentIdempotencyRepository(
        stringRedisTemplate,
        new ObjectMapper()
    );

    PaymentIdempotencyRecord record = PaymentIdempotencyRecord.failure(
        "MCH100001",
        "IDEMP-001",
        "merchantId=MCH100001&requestId=REQ-001&idempotencyKey=IDEMP-001&amount=88.5&currency=CNY",
        "PAYMENT_CREATE_NOT_READY",
        501,
        "not ready"
    );
    repository.save(record, Duration.ofSeconds(30));

    assertThat(repository.find("MCH100001", "IDEMP-001"))
        .contains(record);
  }
}
