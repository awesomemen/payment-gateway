package com.example.payment.gateway.app;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcceptancePageResourceTest {

  @Test
  void shouldContainExtendedAcceptanceScenarios() throws Exception {
    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("static/acceptance/index.html")) {
      assertThat(inputStream).isNotNull();
      String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      assertThat(html)
          .contains("开发验证工作台")
          .contains("环境与诊断")
          .contains("场景预置")
          .contains("运行可靠性验收")
          .contains("复制最新 curl")
          .contains("最近证据")
          .contains("支付查询闭环")
          .contains("支付回调闭环")
          .contains("退款查询闭环")
          .contains("退款回调闭环")
          .contains("交易详情查询")
          .contains("通知重试")
          .contains("支付超时注入");
    }
  }
}
