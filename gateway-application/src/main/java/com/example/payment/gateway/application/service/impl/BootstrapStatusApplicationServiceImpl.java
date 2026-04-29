package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.system.BootstrapStatusResponse;
import com.example.payment.gateway.application.service.BootstrapStatusApplicationService;
import java.util.Arrays;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class BootstrapStatusApplicationServiceImpl implements BootstrapStatusApplicationService {

  private final Environment environment;

  public BootstrapStatusApplicationServiceImpl(Environment environment) {
    this.environment = environment;
  }

  @Override
  public BootstrapStatusResponse currentStatus() {
    return new BootstrapStatusResponse(
        "payment-gateway",
        "BOOTSTRAP",
        "技术脚手架已完成：支付创建主链路已接入安全、幂等、持久化、路由、治理、审计与 Outbox，并已打通本地 Mock Dubbo 下游编排；当前仍非真实生产支付服务。",
        activeProfiles(),
        List.of(
            "多模块父工程与分层依赖方向",
            "Actuator 健康检查",
            "统一 API 响应与异常模型",
            "支付创建主链路与 Mock Dubbo 下游编排",
            "安全验签、时间窗与防重放",
            "Redis 幂等与 Redisson 并发锁",
            "MySQL 请求日志、异常事件与幂等落库",
            "路由查询、治理限流与审计查询接口",
            "RocketMQ Outbox 与静态验收入口页面"
        ),
        List.of(
            "将当前 Mock Dubbo 下游替换为真实支付服务与正式协议转换",
            "补齐异步回调、补偿任务与业务态流转",
            "补齐网关支付单与下游流水关联持久化模型",
            "补强 Testcontainers 与更严格的端到端集成测试",
            "按业务域继续实现支付、退款、查询等后续特性"
        )
    );
  }

  private List<String> activeProfiles() {
    String[] profiles = environment.getActiveProfiles();
    if (profiles.length == 0) {
      return List.of("default");
    }
    return Arrays.asList(profiles);
  }
}
