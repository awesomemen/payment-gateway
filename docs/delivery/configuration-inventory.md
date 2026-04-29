# 配置项说明清单

本文档列出正式交付前最需要确认的配置组，不替代完整的 `application.yml`。

## 1. 安全配置

前缀：`gateway.security`

关键项：

- `config-source`
- `request-expire-seconds`
- `replay-protect-seconds`
- `merchants.*.enabled`
- `merchants.*.signature-key`

当前支持：

- 本地 `application.yml` / 未跟踪覆盖文件
- `gateway-security.json` Nacos 导入

说明：

- 当前代码已通过 `MerchantCredentialProvider` 解耦验签逻辑与具体配置结构
- 当前代码已为 `GatewaySecurityProperties` 加入动态刷新边界

## 2. 下游联调配置

前缀：

- `gateway.downstream.sandbox`
- `dubbo.*`

关键项：

- `gateway.downstream.sandbox.enabled`
- `dubbo.registry.address`
- `dubbo.protocol.port`

说明：

- 本地 sandbox provider 现在可以关闭
- Dubbo consumer 不再强制 `injvm=true`
- 真实联调时应将 `GATEWAY_DOWNSTREAM_SANDBOX_ENABLED=false`

## 3. 治理配置

前缀：`gateway.governance`

关键项：

- `permits-per-minute`
- 限流规则来源
- 是否动态刷新

## 4. 补偿与消息配置

前缀：

- `gateway.messaging.rocketmq`
- `gateway.messaging.notification`
- `gateway.reconcile`

关键项：

- topic / tag / consumer-group
- retry-limit
- retry-delay-seconds
- retry-cron
- processing-cron

## 5. 观测配置

前缀：`gateway.tech.*`

关键项：

- Sentinel Dashboard URL
- Seata Health URL
- SkyWalking Query URL
- Prometheus / Grafana / ELK URL
