# payment-gateway

支付网关服务示例工程。当前仓库的交付目标是：在本地 Docker 拓扑下，先完成剥离业务的最小技术脚手架，再在其上继续接入真实业务能力。

## Current Baseline

当前已经完成并验证的技术脚手架能力包括：

- 支付创建主链路
- 本地 Mock Dubbo 下游编排
- 商户验签
- 幂等控制
- 防重放保护
- MySQL 持久化
- 请求审计与异常事件
- Redis 限流治理
- Nacos 动态治理配置
- JDBC outbox + RocketMQ 发送
- Seata / SkyWalking / Prometheus / Grafana / ELK 组件连通
- HTTP / Dubbo / MQ 技术边界占位

当前边界仍然明确保持为技术脚手架：

- `POST /api/v1/payments` 当前已通过本地 Mock Dubbo 下游返回 `ACCEPTED`，但这仍不代表真实生产支付成功。
- 下一阶段应在此基础上继续替换为真实业务服务，并补齐回调、补偿和业务态流转。

## App-side Docker Config

应用在 Docker 拓扑下使用的中间件连接配置，统一收口在：

- [gateway-app/src/main/resources/application.yml](D:/00-work-files/al-works/workspace-ai/codex/payment-gateway/gateway-app/src/main/resources/application.yml)

其中 `docker` profile 负责以下地址：

- MySQL: `mysql:3306`
- Redis: `redis:6379`
- Nacos: `nacos:8848`
- RocketMQ NameServer: `rocketmq-namesrv:9876`
- Seata Registry: `nacos:8848`
- SkyWalking OAP Zipkin endpoint: `http://skywalking-oap:9411/api/v2/spans`

这意味着应用容器现在只需要：

```yaml
SPRING_PROFILES_ACTIVE: docker
```

不再通过 `java -jar --spring.datasource.url=... --spring.redis.host=...` 之类的启动参数注入中间件连接信息。

## Docker Topology

本地编排文件：

- [docker/local-compose.yml](D:/00-work-files/al-works/workspace-ai/codex/payment-gateway/docker/local-compose.yml)

当前已接入并完成真实验证的服务：

- `mysql`
- `redis`
- `nacos`
- `rocketmq-namesrv`
- `rocketmq-broker`
- `seata`
- `skywalking-oap`
- `skywalking-ui`
- `gateway-app`
- `sentinel-dashboard`
- `prometheus`
- `grafana`
- `elasticsearch`
- `logstash`
- `kibana`

启动命令：

```powershell
docker compose -f docker/local-compose.yml up -d mysql redis nacos rocketmq-namesrv rocketmq-broker seata skywalking-oap skywalking-ui gateway-app
```

停止命令：

```powershell
docker compose -f docker/local-compose.yml down
```

## Build And Regression Test

常用离线命令：

```powershell
mvn --% -o -q -gs .mvn/global-settings.xml -s .mvn/settings.xml test -Dsurefire.failIfNoSpecifiedTests=false
mvn --% -o -q -gs .mvn/global-settings.xml -s .mvn/settings.xml package -DskipTests
mvn --% -o -q -gs .mvn/global-settings.xml -s .mvn/settings.xml install -DskipTests
```

本轮实际执行通过的关键回归命令：

```powershell
.\mvnw.cmd --% -q -gs .mvn/global-settings.xml -s .mvn/settings.xml test -Dsurefire.failIfNoSpecifiedTests=false
.\mvnw.cmd --% -q -gs .mvn/global-settings.xml -s .mvn/settings.xml -pl gateway-app -am clean package -DskipTests
docker compose -f docker/local-compose.yml up -d --build gateway-app
```

说明：

- 当前仓库没有 `PaymentCreateSkyWalkingIntegrationTest`、`PaymentCreateJdbcTransactionalRollbackIntegrationTest` 等集成测试类，旧 README 中相关类名描述已移除。
- 当前可依赖的事实验证包括：全仓单元/模块测试、Docker 运行态检查、真实 HTTP 回归，以及数据库与指标侧证据。

## Human Acceptance

为了让人工验收更直接，仓库现在提供两层交付入口：

- Markdown 指引： [docs/acceptance-ui-guide.md](D:/00-work-files/al-works/workspace-ai/codex/payment-gateway/docs/acceptance-ui-guide.md)
- 页面化验收入口： `http://localhost:18081/acceptance/index.html`

推荐使用方式：

1. 先按文档启动 Docker。
2. 打开验收页，点击核心场景按钮完成支付网关功能验收。
3. 再打开 Nacos、Seata、SkyWalking 页面做中间件和链路的人工核验。

这样验收人员不需要先手工拼签名请求，也不需要先阅读测试代码。

## Runtime Verification

截至 `2026-04-22`，当前运行态已完成以下实时验证：

- `GET /actuator/health` 返回 `200` 且 `status=UP`
- `GET /api/v1/tech/status` 返回 17 项组件全部 `UP`
- `GET /api/v1/governance/config` 与 `POST /api/v1/governance/refresh` 返回 `200`
- `GET /api/v1/audit/summary` 可返回成功/失败请求计数、异常事件数与指标计数
- `/actuator/prometheus` 可看到 `gateway_payment_audit_count_total{result="success|failure"}`

支付创建主链路的核心场景实时验证结果：

- 成功请求：HTTP `200`，业务码 `SUCCESS`，`data.status=ACCEPTED`
- 合法重复请求：两次均为 HTTP `200`，且 `gatewayPaymentId` 相同
- 冲突幂等：HTTP `409`，业务码 `IDEMPOTENCY_CONFLICT`
- 重放攻击：HTTP `401`，业务码 `REQUEST_REPLAYED`
- 错误签名：HTTP `401`，业务码 `SIGNATURE_INVALID`

数据库侧当前可见证据：

- `gateway_request_log` 已记录成功与失败请求
- `gateway_exception_event` 已记录重放、错签等异常事件
- `gateway_mq_outbox` 最新记录 `send_status=1`
- 最新 Outbox 载荷已包含 `downstreamPaymentId`
- `gateway_idempotency_record.request_hash` 已调整到 `VARCHAR(512)` 以承载当前业务指纹长度

## Key Files

- App config: [gateway-app/src/main/resources/application.yml](D:/00-work-files/al-works/workspace-ai/codex/payment-gateway/gateway-app/src/main/resources/application.yml)
- Docker compose: [docker/local-compose.yml](D:/00-work-files/al-works/workspace-ai/codex/payment-gateway/docker/local-compose.yml)
- Seata config: [docker/seata/conf/application.yml](D:/00-work-files/al-works/workspace-ai/codex/payment-gateway/docker/seata/conf/application.yml)
- SkyWalking OAP config: [docker/skywalking/oap/application.yml](D:/00-work-files/al-works/workspace-ai/codex/payment-gateway/docker/skywalking/oap/application.yml)
