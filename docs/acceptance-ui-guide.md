# 人工验收指引

这份指引面向验收人员和开发人员，不要求先读代码。当前 `http://localhost:18081/acceptance/index.html` 已不只是简单验收页，而是一个开发验证工作台。按下面顺序打开页面、选择预置、点击按钮、核对结果即可。

如果你需要一份更偏“如何上手操作”的短手册，可同时参考同目录下的：

- [验收工作台操作指南](./acceptance-operation-guide.md)

## 1. 验收入口

先启动本地 Docker 拓扑：

```powershell
docker compose -f docker/local-compose.yml up -d mysql redis nacos rocketmq-namesrv rocketmq-broker seata skywalking-oap skywalking-ui gateway-app
```

然后打开这 5 个页面：

| 页面 | 地址 | 用途 | 通过标准 |
|---|---|---|---|
| 验收页 | `http://localhost:18081/acceptance/index.html` | 开发验证工作台：环境诊断、单功能验证、组合场景验收、证据导出 | 页面能打开，按钮可执行，请求结果符合预期 |
| 应用健康页 | `http://localhost:18081/actuator/health` | 确认应用已启动 | 页面返回 `{"status":"UP"...}` |
| Nacos 控制台 | `http://localhost:8848/nacos` | 看治理配置和 Seata 注册 | 控制台能打开 |
| Seata 健康页 | `http://localhost:7091/health` | 看事务协调器是否可用 | 页面显示 `ok` |
| SkyWalking UI | `http://localhost:18080` | 看链路是否上报 | 页面能打开，能看到 `payment-gateway` |

## 2. 在验收页上完成核心功能验证

打开：

- `http://localhost:18081/acceptance/index.html`

页面里已经内置了本地验收用测试商户、测试签名、随机 nonce 和时间戳，不需要手工拼参数。页面会自动记住最近一次支付单 / 退款单，便于串联查询、回调和交易检索。

建议先理解页面的 4 个区域：

- `环境与诊断`：检查应用健康、Prometheus、Nacos / Seata / SkyWalking，并查看最近一次套件进度。
- `场景预置`：直接切换 `smoke / processing / reject / timeout / error / refund-processing / rate-limit`，也可以手工修改 `requestPrefix`、金额、状态和目标单号。
- `单功能验证`：按按钮执行支付、退款、查询、回调、审计、重试、异常注入。
- `最近证据`：复制最近一次 `curl`、请求体、响应体和上下文 ID，方便查日志、查数据库和复现实验。

建议按以下顺序操作：

1. 在 `场景预置` 中先选择 `smoke`，然后点击 `应用预置`
期望结果：表单填入默认联调参数，页面后续按钮都基于这组可编辑参数运行。

2. 点击 `检查应用健康`
期望结果：显示 `UP`。

3. 点击 `成功请求`
期望结果：
- HTTP `200`
- 业务码 `SUCCESS`
- `data.status=ACCEPTED`

4. 点击 `重复请求`
期望结果：
- 两次请求都返回 HTTP `200`
- 两次返回的 `gatewayPaymentId` 相同

5. 点击 `冲突幂等`
期望结果：
- 第二次请求返回 HTTP `409`
- 业务码 `IDEMPOTENCY_CONFLICT`

6. 点击 `重放攻击`
期望结果：
- 第二次请求返回 HTTP `401`
- 业务码 `REQUEST_REPLAYED`

7. 点击 `错误签名`
期望结果：
- 返回 HTTP `401`
- 业务码 `SIGNATURE_INVALID`

8. 点击 `支付查询闭环`
期望结果：
- 先完成一次支付创建；
- 随后查询返回 HTTP `200`
- 业务码 `SUCCESS`
- `data.status=SUCCEEDED`

9. 点击 `支付回调闭环`
期望结果：
- 先创建一笔 `PROCESSING` 支付；
- 页面会自动读取交易详情中的 `downstreamOrderId`；
- 回调后再查一次，最终状态为 `SUCCEEDED`

10. 点击 `退款查询闭环`
期望结果：
- 基于最近一笔支付自动创建退款；
- 退款查询返回 HTTP `200`
- 业务码 `SUCCESS`
- `data.status=SUCCEEDED`

11. 点击 `退款回调闭环`
期望结果：
- 先创建一笔 `PROCESSING` 退款；
- 自动回调后再查询，最终状态为 `SUCCEEDED`

12. 点击 `交易详情查询`
期望结果：
- 返回 HTTP `200`
- 业务码 `SUCCESS`
- `data.bizType` 为 `PAY` 或 `REFUND`

13. 点击 `交易审计检索`
期望结果：
- 返回 HTTP `200`
- 业务码 `SUCCESS`
- `requestLogs` 或 `exceptionEvents` 中能看到最近一次动作留下的记录

14. 点击 `检查审计指标`
期望结果：
- 成功指标能看到 `gateway.payment.audit.count`
- 失败指标能看到 `gateway.payment.audit.count`
- 页面中会显示当前计数值

15. 点击 `Outbox 重放` 与 `通知重试`
期望结果：
- 两个接口都返回 HTTP `200`
- 业务码 `SUCCESS`
- 返回体中能看到本次扫描 / 成功 / 失败统计

16. 在 `最近证据` 中依次点击：
   - `复制最新 curl`
   - `复制最新请求`
   - `复制最新响应`
   - `复制最近上下文`
期望结果：
- 都能复制到剪贴板；
- 最近上下文中能看到最近一次 `requestId / gatewayPaymentId / gatewayRefundId / downstreamId`。

如果想一次跑完：

- 点击 `运行核心验收`：会顺序执行支付创建、安全、幂等、查询与回调闭环；
- 点击 `运行扩展验收`：会在核心验收后继续执行退款闭环、交易查询、审计汇总和消息重试。
- 点击 `运行可靠性验收`：会继续执行拒绝 / 超时 / 异常注入、治理刷新、限流验证、处理中补偿、Outbox 重放和通知重试。

页面会对每一步打出 `PASS / FAIL / WARN` 标记，并在 `环境与诊断` 区展示当前 `Suite / Step / Progress`。

## 3. 页面内异常注入测试

验收页还提供了 3 个支付异常注入按钮，用于验证网关对下游异常的分类是否稳定。建议在 `场景预置` 里先切到对应预置，再执行按钮，这样页面会自动带上更清晰的 `requestPrefix` 和上下文标记：

1. 点击 `支付拒绝注入`
期望结果：
- HTTP `422`
- 业务码 `DOWNSTREAM_REJECTED`

2. 点击 `支付超时注入`
期望结果：
- HTTP `504`
- 业务码 `DOWNSTREAM_TIMEOUT`

3. 点击 `支付异常注入`
期望结果：
- HTTP `502`
- 业务码 `DOWNSTREAM_SERVICE_ERROR`

## 4. 在 Nacos 页面核对治理与注册状态

打开：

- `http://localhost:8848/nacos`

### 3.1 检查 Seata 是否注册成功

在控制台进入服务列表，搜索：

- `seata-server`

期望结果：

- 存在 `SEATA_GROUP@@seata-server`
- 有 1 个健康实例
- 实例端口是 `8091`

### 3.2 检查治理配置是否可人工调整

在配置列表中检查是否存在：

- `Data ID`: `gateway-governance.json`
- `Group`: `DEFAULT_GROUP`

如果不存在，可手工新建，内容如下：

```json
{"permitsPerMinute":1}
```

保存后回到验收页：

1. 点击 `刷新治理配置`
2. 点击 `治理限流验证`

期望结果：

- 刷新接口返回 `permitsPerMinute=1`
- 限流验证中至少有 1 次请求返回 HTTP `429`
- 业务码为 `RATE_LIMITED`

注意：

- 这个场景受分钟级限流窗口影响。
- 如果前面已经连续发过很多请求，建议等一分钟后再做这一步，结果会更清晰。

## 5. 在 SkyWalking UI 上核对链路

打开：

- `http://localhost:18080`

建议按下面路径检查：

1. 进入服务列表，确认存在 `payment-gateway`
2. 打开最近 15 分钟数据
3. 查看入口接口是否出现：
   - `POST /api/v1/payments`
   - `POST /api/v1/payments/query`
   - `POST /api/v1/refunds`

期望结果：

- 能看到服务 `payment-gateway`
- 能看到最近发起的支付创建、支付查询或退款调用
- 调用耗时、状态和拓扑能正常展示

如果 SkyWalking 页面暂时没有数据，先回到验收页重新执行一次 `运行扩展验收` 或 `运行可靠性验收`，再刷新 SkyWalking 页面。

## 6. 验收结论建议

人工验收可以按下面标准给结论：

- 核心接口可用：开发验证工作台中的支付主链路和退款/查询闭环场景全部 `PASS`
- 应用状态正常：健康页返回 `UP`
- 分布式事务可发现：Nacos 里能看到 `seata-server`
- 观测链路正常：SkyWalking 里能看到 `payment-gateway`
- 动态治理可操作：Nacos 配置可修改，刷新后可触发限流
- 补偿与消息入口可访问：Outbox 重放、通知重试接口都返回 `SUCCESS`
- 证据可导出：页面的 `复制最新 curl / 请求 / 响应 / 上下文` 可直接用于日志和数据库排查

满足以上 6 条，即可认为当前交付具备“可打开、可操作、可观察、可验证”的业务级人工验收条件。
