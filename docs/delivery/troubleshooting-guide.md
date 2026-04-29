# 常见问题排查说明

## 1. 支付创建返回下游相关错误

先检查：

- `gateway.downstream.sandbox.enabled`
- Dubbo 注册中心地址
- 目标 provider 是否注册
- 路由是否命中预期接口

## 2. 验签失败

先检查：

- 商户号是否正确
- `gateway.security` 配置来源是否正确
- 商户密钥是否已更新到目标环境
- 请求时间戳和 nonce 是否符合要求

## 3. Nacos 配置未生效

先检查：

- Data ID
- Group
- Namespace
- 是否开启 `refreshEnabled=true`
- `gateway-security.json` 与 `gateway-governance.json` 是否都已导入

## 4. MQ 重试或消费异常

先检查：

- NameServer / Broker
- Topic / Tag / Consumer Group
- `gateway_mq_outbox`
- `gateway_message_consume_record`
- `gateway_request_log` 中 `api_code=CONSUME`

## 5. 事务或补偿结果异常

先检查：

- Seata 注册状态
- `gateway_payment_order`
- `gateway_refund_order`
- `gateway_exception_event`
- 补偿与重试接口返回值
