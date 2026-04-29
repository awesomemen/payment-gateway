# 验收工作台操作指南

这份文档是 [人工验收指引](./acceptance-ui-guide.md) 的配套操作手册，重点解决“进入页面后先点什么、怎么看结果、失败后去哪里查”。

## 1. 适用范围

适用于当前本地 Docker 环境下的开发验证工作台：

- 页面地址：`http://localhost:18081/acceptance/index.html`
- 运行前提：`gateway-app`、MySQL、Redis、Nacos、RocketMQ、Seata、SkyWalking 已启动
- 当前下游形态：本地 stateful sandbox contract，不是真实外部支付服务

## 2. 使用前检查

先确认下面 4 项：

1. `docker compose -f docker/local-compose.yml ps`
   重点看 `gateway-app` 是否为 `healthy`
2. 打开 `http://localhost:18081/actuator/health`
   结果应包含 `status=UP`
3. 打开 `http://localhost:8848/nacos`
   页面可访问即可
4. 打开 `http://localhost:18080`
   SkyWalking UI 可访问即可

如果第 1 步不正常，不建议直接点验收按钮，先恢复环境。

## 3. 页面区域怎么用

页面主要分 4 个区：

### 3.1 环境与诊断

这里主要看：

- 应用健康
- Prometheus
- Nacos / Seata / SkyWalking 快捷入口
- 当前 suite 的 `Suite / Step / Progress`

建议每次验收前先点一次 `检查应用健康`。

### 3.2 场景预置

这里决定当前按钮运行时使用哪组参数。

常用预置：

- `smoke`
  用于最普通的成功路径 smoke check
- `processing`
  用于验证支付处理中、后续回调和查询闭环
- `reject`
  用于下游拒绝验证
- `timeout`
  用于超时验证
- `error`
  用于下游系统异常验证
- `refund-processing`
  用于退款处理中和退款回调
- `rate-limit`
  用于治理限流验证

推荐用法：

1. 先选预置
2. 点击 `应用预置`
3. 如果要定向联调，再手工改 `requestPrefix`、金额、状态或目标单号

### 3.3 单功能验证

这里适合做单点检查。

推荐顺序：

1. `成功请求`
2. `重复请求`
3. `冲突幂等`
4. `重放攻击`
5. `错误签名`
6. `支付查询闭环`
7. `支付回调闭环`
8. `退款查询闭环`
9. `退款回调闭环`
10. `交易详情查询`
11. `交易审计检索`
12. `Outbox 重放`
13. `通知重试`

### 3.4 最近证据

这里是排障效率最高的区域。

跑完一个动作后，优先用这 4 个按钮：

- `复制最新 curl`
- `复制最新请求`
- `复制最新响应`
- `复制最近上下文`

推荐做法：

- 页面失败时，先复制 `最新响应`
- 再复制 `最近上下文`
- 用里面的 `requestId / gatewayPaymentId / gatewayRefundId / downstreamId` 去查日志或数据库

## 4. 三种推荐用法

### 4.1 快速冒烟

适合确认“系统今天还能不能正常工作”。

步骤：

1. 选择 `smoke`
2. 点击 `应用预置`
3. 点击 `检查应用健康`
4. 点击 `成功请求`
5. 点击 `支付查询闭环`
6. 点击 `交易详情查询`

通过标准：

- 都返回 HTTP `200`
- 业务码为 `SUCCESS`
- 查询闭环最终为 `SUCCEEDED`

### 4.2 回归验证

适合功能改动后做一轮完整回归。

步骤：

1. 选择 `smoke`
2. 点击 `应用预置`
3. 点击 `运行核心验收`
4. 核心通过后，点击 `运行扩展验收`

通过标准：

- 页面结果列表无 `FAIL`
- `Suite / Step / Progress` 能正常推进到结束

### 4.3 异常与恢复验证

适合验证网关的可靠性边界。

步骤：

1. 选择对应预置：`reject / timeout / error / rate-limit`
2. 点击 `应用预置`
3. 先执行单项异常按钮，确认语义正确
4. 再执行 `运行可靠性验收`

重点看：

- `支付拒绝注入 -> 422 / DOWNSTREAM_REJECTED`
- `支付超时注入 -> 504 / DOWNSTREAM_TIMEOUT`
- `支付异常注入 -> 502 / DOWNSTREAM_SERVICE_ERROR`
- `治理限流验证 -> 至少 1 次 429 / RATE_LIMITED`
- `处理中补偿 / Outbox 重放 / 通知重试` 是否返回 `SUCCESS`

## 5. 常见失败时先查哪里

### 5.1 页面打不开

先查：

- `docker compose -f docker/local-compose.yml ps`
- `http://localhost:18081/actuator/health`

### 5.2 按钮返回 404 或资源不存在

先查：

- 当前容器是否真的是最新镜像
- `gateway-app` 是否刚重建完成

### 5.3 支付或退款闭环没推进

先查：

- 页面里的 `最近上下文`
- `gateway_payment_order` / `gateway_refund_order`
- `/api/v1/transactions/detail`
- `/api/v1/transactions/audit`

### 5.4 重试或补偿没生效

先查：

- `Outbox 重放`
- `通知重试`
- `处理中补偿`
- `gateway_request_log`
- `gateway_exception_event`
- `gateway_message_consume_record`

### 5.5 限流没有触发

先查：

- Nacos 中 `gateway-governance.json`
- 是否刚刷新治理配置
- 是否还在同一分钟窗口内反复测试

## 6. 建议的验收结论写法

如果要给开发或测试留结论，建议至少记录：

- 本次使用的预置或 suite
- 最后一次 `requestId`
- 最后一次 `gatewayPaymentId / gatewayRefundId`
- 是否通过
- 若失败，失败按钮名称和错误码

最简模板：

```text
验收类型：核心验收 / 扩展验收 / 可靠性验收
结果：PASS / FAIL
关键 requestId：REQ-...
关键单号：GP... / GR...
失败点：无 / 支付超时注入
错误码：无 / DOWNSTREAM_TIMEOUT
```
