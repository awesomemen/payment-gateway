# 本地已沉淀证据索引

本文档记录当前仓库已经完成的本地验证证据类型，方便后续整理成正式交付包。

## 已有本地证据

- `gateway-infrastructure` 定向测试：
  - `DownstreamExternalContractWiringTest`
  - `MockPaymentCreateFacadeImplTest`
  - `MockPaymentQueryFacadeImplTest`
  - `MockRefundFacadeImplTest`
  - `DubboDownstreamPaymentCreateGatewayTest`
  - `DubboDownstreamPaymentQueryGatewayTest`
- `gateway-security` 定向测试：
  - `DefaultPaymentRequestSecurityValidatorTest`
  - `PropertiesMerchantCredentialProviderTest`
- 应用烟测：
  - `PaymentGatewayApplicationTest`
- 本地可靠性套件：
  - `scripts/local-reliability-suite.ps1`
  - 成功证据目录：
    - `.tmp-reliability/20260424-113208`
  - 摘要结论：
    - `redis-outage => PASS`
    - `mysql-outage => PASS`
    - `rocketmq-broker-outage => PASS`
    - `seata-outage => REVIEW`

## 当前已验证结论

- 本地 sandbox provider 可通过 `gateway.downstream.sandbox.enabled` 开关关闭
- Dubbo consumer 不再强制 `injvm=true`
- 商户验签配置已支持统一 provider 抽象
- `gateway-security.json` 已进入本地 / docker profile 的 Nacos 导入链路
- 当前已具备一套可复跑的本地组件故障探针脚本，用于沉淀 Redis / MySQL / RocketMQ Broker / Seata 场景证据

## 待补为正式证据的部分

- 真实外部 provider 联调请求与响应
- 真实联调环境中的数据库、MQ、观测平台截图
- 正式验收环境的最终结论记录
