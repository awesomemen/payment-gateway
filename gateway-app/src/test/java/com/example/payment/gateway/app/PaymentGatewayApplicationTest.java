package com.example.payment.gateway.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration,"
        + "org.redisson.spring.starter.RedissonAutoConfigurationV2"
})
class PaymentGatewayApplicationTest {

  @Test
  void shouldLoadApplicationContext() {
  }
}
