package com.example.payment.gateway.infrastructure.idempotency;

import com.example.payment.gateway.common.idempotency.PaymentIdempotencyLock;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class RedissonPaymentIdempotencyLockManagerTest {

  @Container
  static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.2.13").withExposedPorts(6379);

  private RedissonClient redissonClient;

  @AfterEach
  void tearDown() {
    if (redissonClient != null) {
      redissonClient.shutdown();
    }
  }

  @Test
  void shouldAcquireAndReleaseLock() {
    Config config = new Config();
    config.useSingleServer().setAddress("redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
    redissonClient = Redisson.create(config);

    RedissonPaymentIdempotencyLockManager lockManager = new RedissonPaymentIdempotencyLockManager(redissonClient);

    Optional<PaymentIdempotencyLock> firstLock = lockManager.tryLock(
        "MCH100001",
        "IDEMP-001",
        Duration.ZERO,
        Duration.ofSeconds(5)
    );
    Optional<PaymentIdempotencyLock> secondLock = lockManager.tryLock(
        "MCH100001",
        "IDEMP-001",
        Duration.ZERO,
        Duration.ofSeconds(5)
    );

    assertThat(firstLock).isPresent();
    assertThat(secondLock).isEmpty();

    firstLock.orElseThrow().release();

    assertThat(lockManager.tryLock(
        "MCH100001",
        "IDEMP-001",
        Duration.ZERO,
        Duration.ofSeconds(5)
    )).isPresent();
  }
}
