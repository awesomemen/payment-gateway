# Findings

## Repository Reality
- 当前仓库已经具备 `gateway-common / gateway-api / gateway-domain / gateway-security / gateway-governance / gateway-observability / gateway-infrastructure / gateway-application / gateway-web / gateway-app` 多模块代码基线。
- `gateway-app` 已提供 Spring Boot 3.5.0 应用入口、Dockerfile、`application.yml`、静态验收页与基础运行配置。

## AGENTS Contract Highlights
- 项目必须按 `gateway-app / gateway-api / gateway-web / gateway-application / gateway-domain / gateway-infrastructure / gateway-security / gateway-governance / gateway-observability / gateway-common` 分层组织。
- 安全、限流、事务、锁、审计、指标、协议边界均属高风险区域，只能做最小必要修改。
- 本地开发优先使用 Docker Compose，应用配置通过 `local` 或 `docker` profile + 环境变量/未跟踪覆盖文件注入。

## Docker Topology
- `docker/local-compose.yml` 已定义 `mysql`、`redis`、`nacos`、`rocketmq`、`seata`、`skywalking`、`sentinel-dashboard`、`prometheus`、`grafana`、`elk` 及 `gateway-app`。
- Compose 中 `gateway-app` 依赖根目录存在 `gateway-app/Dockerfile`，健康检查为 `GET /actuator/health`。

## Development Implication
- 第一阶段必须先补齐 Maven 父工程、模块目录和 `gateway-app` 最小可运行应用，否则 Docker Compose 中的应用服务无法落地。
- 同时需要提供最小配置文件和基础 Web 层入口，让后续安全、领域、编排、治理能力能按既定分层继续迭代。

## Official Stack Verification
- 官方 Spring Cloud Alibaba 版本说明显示：`2025.0.0.0 -> Spring Cloud 2025.0.0 -> Spring Boot 3.5.0`。
- 官方组件适配关系显示：`2025.0.0.0` 对应 `Sentinel 1.8.9`、`Nacos 3.0.3`、`RocketMQ 5.3.1`、`Seata 2.5.0`。
- 官方 Nacos 快速开始明确说明：从 `2025.0.x` 起，如果使用 `public` 或空字符串命名空间，Nacos Server 需要升级到 `3.x`。
- 本轮已将父 POM 调整为 `Spring Boot 3.5.0 + Spring Cloud 2025.0.0 + Spring Cloud Alibaba 2025.0.0.0`。
- 本轮已将 `docker/local-compose.yml` 中的 Nacos 镜像升级为 `nacos/nacos-server:v3.0.3`，但尚未重新拉起容器验证运行时状态。

## Verified Bootstrap Baseline
- Maven 父工程、多模块分层和基础依赖方向已建立。
- 仓库内 `mvnw.cmd` 已修复到 Apache Maven Wrapper 3.3.2，并锁定 Maven 3.9.9。
- 当前应用可通过 `gateway-app` 启动，暴露 `actuator/health`、`/api/v1/bootstrap/status` 和静态验收页。
- 支付创建入口 `POST /api/v1/payments` 当前明确返回 `501 PAYMENT_CREATE_NOT_READY`，没有伪造业务成功。

## Runtime Drift (Latest Check)
- 该漂移问题已在本轮通过重建容器解决：当前 `gateway-nacos` 为 `v3.0.3`，`gateway-app` 为最新源码构建镜像，`/api/v1/bootstrap/status` 与 `/api/v1/tech/status` 均已可用。

## Runtime Verification (Resolved)
- `docker/local-compose.yml` 需要为 Nacos 3 补齐 `NACOS_AUTH_TOKEN`、`NACOS_AUTH_IDENTITY_KEY`、`NACOS_AUTH_IDENTITY_VALUE`，即使本地关闭鉴权，启动脚本仍会检查这些变量。
- Nacos 3 的 Compose 健康检查路径改为 `http://localhost:8848/nacos/` 更稳妥；旧的 `/nacos/actuator/health` 在当前镜像下返回 404。
- `gateway-app` 镜像构建依赖稳定产出 `gateway-app.jar`；已通过 `gateway-app/pom.xml` 的 `finalName` 与 `spring-boot-maven-plugin repackage` 固化。
- `bladex/sentinel-dashboard:1.8.8` 在当前镜像内实际监听 `8858`，因此 Compose 宿主机映射与应用 `docker` profile 里的 dashboard 地址都要对齐到 `8858`。
- `/acceptance/index.html` 静态页可直接访问；为了补齐目录入口体验，已增加 `/acceptance` 与 `/acceptance/` 到 `index.html` 的重定向控制器。

## Verified Runtime Baseline
- `docker compose -f docker/local-compose.yml ps` 显示 `gateway-app / mysql / redis / nacos / rocketmq / seata / skywalking / sentinel-dashboard / prometheus / grafana / elasticsearch / logstash / kibana` 均已启动，其中关键依赖为 healthy。
- `GET /actuator/health` 返回 `UP`，`GET /api/v1/bootstrap/status` 返回技术脚手架阶段说明，`GET /api/v1/tech/status` 返回 17 项组件探针全部 `UP`。
- `POST /api/v1/payments` 对完整示例请求仍然返回 `501 PAYMENT_CREATE_NOT_READY`，说明当前仍保持“剥离业务的最小技术脚手架”边界。
- `GET /acceptance/` 现返回 `302 -> /acceptance/index.html`，静态验收入口已可直接使用。
- `elasticsearch` 在 `tech status` 中返回 `status=yellow`，这是单节点开发模式下可接受状态，不影响当前脚手架连通性判定。

## Priority Rationale For Remaining Features
- `docs/agents/AGENTS.project-overview.md` 明确支付网关首先是安全边界、协议边界、流量治理边界，而不是普通 CRUD 服务。
- `docs/agents/AGENTS.architecture-guardrails.md` 要求安全校验必须先于路由与业务编排，并单独区分防重放、幂等、分布式锁和事务边界。
- `README.md` 已把“商户签名校验、幂等控制、防重放保护、审计/异常事件、Redis 限流治理”列为支付创建主链路的核心能力，而非外围增强项。
- `docs/acceptance-ui-guide.md` 的验收场景包含幂等冲突、治理限流、审计指标和动态配置刷新，这意味着下一阶段必须优先把这些“技术语义”落到真实请求路径。
- 因此剩余功能的优先级应是：
  - 先做安全入口链路：merchant 配置读取、验签、时间窗、防重放。
  - 再做业务语义保护：幂等、并发锁、重复请求返回。
  - 再做持久化基线：支付请求、审计、异常事件落库。
  - 最后逐步接入路由编排、治理能力、异步通知和观测自动化。

## Security Slice 1 Implementation Notes
- `gateway-security` 已补齐最小安全组件：
  - `GatewaySecurityProperties` 负责读取 `gateway.security.*`；
  - `DefaultPaymentRequestSecurityValidator` 负责商户识别、时间窗校验、签名校验、防重放；
  - `InMemoryReplayProtectionStore` 作为当前脚手架阶段的防重放存储实现；
  - 签名算法当前固定为基于 `merchantId/requestId/idempotencyKey/amount/currency/requestTime/nonce` 的 `HmacSHA256` 十六进制小写串。
- `gateway-app/src/main/resources/application.yml` 已提供本地样例商户 `MCH100001` 和默认签名密钥 `demo-signature-key`，便于脚手架回归验证。
- `BootstrapPaymentCreateApplicationService` 现在会先把 `PaymentCreateRequest` 转成 `PaymentCreateCommand`，再调用安全校验器；只有安全校验通过后，才继续返回当前脚手架边界错误 `PAYMENT_CREATE_NOT_READY`。
- 当前实现边界：
  - 商户配置仍是配置文件驱动，不是 Nacos/MySQL 正式接入；
  - 防重放仍是进程内内存存储，不是 Redis/持久化存储；
  - 这意味着当前实现满足“最小技术闭环”，但不满足多实例或重启后的正式运行要求。

## Security Slice 1 Verification
- 单元测试通过：
  - `DefaultPaymentRequestSecurityValidatorTest` 验证了未知商户、错误签名、时间窗过期、nonce 重放、合法请求。
  - `BootstrapPaymentCreateApplicationServiceTest` 验证了应用服务会先执行安全校验，再落到 `PAYMENT_CREATE_NOT_READY`。
- 回归测试通过：
  - `PaymentControllerTest`、`AcceptancePageControllerTest`、`PaymentGatewayApplicationTest` 均通过。
- 真实 HTTP 回归通过：
  - 正确签名请求返回 `501 PAYMENT_CREATE_NOT_READY`；
  - 错误签名请求返回 `401 SIGNATURE_INVALID`；
  - 同一正确签名请求重复提交，第二次返回 `409 REPLAY_ATTACK_DETECTED`。

## Idempotency Slice 1 Implementation Notes
- `gateway-common` 已抽出幂等基础抽象，避免基础设施层反向依赖应用层：
  - `PaymentIdempotencyRepository`
  - `PaymentIdempotencyLockManager`
  - `PaymentIdempotencyLock`
  - `PaymentIdempotencyRecord`
  - `StoredPaymentResult`
- `gateway-application` 已新增 `GatewayIdempotencyProperties` 与 `PaymentCreateIdempotencyCoordinator`：
  - 先查幂等记录；
  - 未命中时申请 Redisson 锁；
  - 执行业务占位处理器后，把成功或失败结果都存入幂等记录；
  - 同指纹重复请求返回已保存的语义结果；
  - 同 `idempotencyKey` 但不同业务指纹请求返回 `409 IDEMPOTENCY_CONFLICT`。
- `PaymentCreateCommand.idempotencyFingerprint()` 当前只包含 `merchantId/requestId/idempotencyKey/amount/currency`，刻意不包含 `requestTime/nonce/signature`，用于区分“合法重试”和“安全重放”。
- `gateway-infrastructure` 已补最小双实现：
  - `InMemoryPaymentIdempotencyRepository` 与 `InMemoryPaymentIdempotencyLockManager` 供默认 profile 和测试上下文使用；
  - `RedisPaymentIdempotencyRepository` 与 `RedissonPaymentIdempotencyLockManager` 供 `local/docker` profile 使用。
- Redis 键约定已固化：
  - 幂等记录：`gateway:idempotency:{merchantId}:{idempotencyKey}`
  - 并发锁：`gateway:lock:idempotency:{merchantId}:{idempotencyKey}`
- 当前脚手架边界保持不变：幂等记录会缓存 `PAYMENT_CREATE_NOT_READY` 这类“业务尚未就绪”的结果，用于验证幂等机制本身，而不是伪造成功支付。

## Idempotency Slice 1 Verification
- 目标测试通过：
  - `PaymentCreateIdempotencyCoordinatorTest`
  - `BootstrapPaymentCreateApplicationServiceTest`
  - `PaymentControllerTest`
  - `AcceptancePageControllerTest`
  - `PaymentGatewayApplicationTest`
- 基础设施侧已新增 `RedisPaymentIdempotencyRepositoryTest` 与 `RedissonPaymentIdempotencyLockManagerTest`。
- 当前机器上的 Testcontainers 存在 Docker 探测兼容问题：测试执行时无法稳定识别本机 Docker 环境，因此这两条基础设施测试使用了 `@Testcontainers(disabledWithoutDocker = true)`，在无法探测时自动跳过，不阻断主验证链。
- 已通过 Redis 实际数据确认幂等结果不是 JVM 内存态：存在键 `gateway:idempotency:MCH100001:IDEMP-20260421-2001`，内容为之前真实请求产生的失败语义结果。
- 已完成应用重启后的真实 HTTP 回归：
  - 重启 `gateway-app` 后，对相同业务语义但全新 `nonce/requestTime` 的请求，返回 `501 PAYMENT_CREATE_NOT_READY`，说明命中了已保存的幂等结果；
  - 对相同 `idempotencyKey` 但不同 `requestId/amount` 的请求，返回 `409 IDEMPOTENCY_CONFLICT`，说明冲突语义在重启后仍然成立。

## Persistence / Routing / Governance / Outbox Slice
- 当前代码已存在并接入以下组件：
  - `JdbcGatewayRouteRepository`
  - `JdbcPaymentRequestLogRepository`
  - `JdbcPaymentExceptionEventRepository`
  - `JdbcPaymentIdempotencyJournalRepository`
  - `CompositePaymentIdempotencyRepository`
  - `RedisPaymentGovernanceGuard`
  - `RocketMqPaymentOutboxPublisher`
  - `PaymentAuditController`
  - `PaymentGovernanceController`
- 这意味着支付创建主链路已不再停留在“仅安全 + 幂等”的阶段，而是具备了持久化、治理查询与消息发送骨架。

## Runtime Drift And Defect Resolution
- 2026-04-22 实时回归中再次发现“源码领先于运行态”的风险：容器中的 `bootstrap/status` 与真实能力存在描述漂移，因此后续每次阶段性完成后仍需要显式重建 `gateway-app`。
- 真实支付请求首次返回 `500 INTERNAL_ERROR` 的根因并非路由或 RocketMQ，而是：
  - `gateway_idempotency_record.request_hash` 列宽为 `VARCHAR(128)`；
  - 当前代码把原始业务指纹字符串直接写入 `request_hash`；
  - 指纹长度超过 128 字符时，MySQL 抛出 `Data too long for column 'request_hash'`。
- 该问题已通过两部分修复：
  - 初始化脚本 `docker/mysql/init/01-schema.sql` 将 `request_hash` 调整为 `VARCHAR(512)`；
  - 运行态数据库已执行 `ALTER TABLE gateway_idempotency_record MODIFY request_hash VARCHAR(512) NULL`。
- 同时为提升定位效率，`GatewayExceptionHandler` 已补充 `GatewayException` 与未知异常日志，避免再次出现 `500` 无堆栈问题。

## Verified Runtime Snapshot (2026-04-22)
- `GET /actuator/health` 返回 `UP`。
- `GET /api/v1/tech/status` 返回 17 项组件全部 `UP`。
- `GET /api/v1/governance/config` 与 `POST /api/v1/governance/refresh` 返回 `permitsPerMinute=60`。
- `GET /api/v1/audit/summary` 返回成功/失败请求数、异常事件数与审计指标计数。
- `/actuator/prometheus` 当前可见：
  - `gateway_payment_audit_count_total{result="success"}`
  - `gateway_payment_audit_count_total{result="failure"}`

## Verified Acceptance Snapshot (2026-04-22)
- 成功请求：HTTP `200`，业务码 `SUCCESS`，`data.status=ACCEPTED`。
- 合法重复请求：两次 HTTP `200`，且 `gatewayPaymentId` 相同。
- 冲突幂等：HTTP `409`，业务码 `IDEMPOTENCY_CONFLICT`。
- 重放攻击：HTTP `401`，业务码 `REQUEST_REPLAYED`。
- 错误签名：HTTP `401`，业务码 `SIGNATURE_INVALID`。
- 最新数据库证据：
  - `gateway_request_log` 已落成功/失败请求；
  - `gateway_exception_event` 已落重放与错签事件；
  - `gateway_mq_outbox` 最新记录 `send_status=1`。

## Documentation Alignment
- `README.md` 之前存在对不存在的集成测试类名的引用，属于文档超前于事实。
- 当前 README 应只保留本轮真实执行过的测试命令、真实 HTTP 回归结果与数据库/指标证据。
- `BootstrapStatusApplicationServiceImpl` 也需要同步反映“技术脚手架已完成”而非仍处于早期骨架阶段。

## Business Priority Plan (Post-Scaffold)
- 从 `AGENTS.project-overview.md` 看，支付网关的首要职责仍是“统一入口 + 安全边界 + 协议转换 + 路由转发 + 稳定性保护”，因此业务阶段的第一优先级不是“多做几个接口”，而是把现有支付创建入口接成真实下游调用。
- 从 `README.md` 当前基线看，技术侧已经覆盖验签、幂等、防重放、持久化、治理、Outbox 与观测，因此最有价值的下一步是让这些能力承载真实支付交易，而不是继续堆技术底座。
- 从 `docs/acceptance-ui-guide.md` 看，当前人工验收只覆盖“支付创建受理”而没有覆盖“最终支付结果闭环”；这说明业务阶段最先缺失的是回调、状态推进、查询兜底，而不是退款或更外围的运营功能。
- 从 `AGENTS.architecture-guardrails.md` 看，回调、补偿、消息、事务、幂等等控制手段必须边界清晰，因此“支付创建 -> 结果闭环 -> 补偿恢复”应按链路顺序实施，而不是把退款、查询、补偿并行摊开做。

### Recommended Priority Order
1. `P0: 真实支付创建编排`
   - 价值最高，因为它把当前 `ACCEPTED` 占位真正替换成可落地交易能力。
   - 也是后续回调、查询、补偿、退款的共同前提。
2. `P1: 支付结果闭环`
   - 支付创建一旦接入真实下游，就必须尽快补齐回调、状态流转和结果查询，否则链路只完成“发起”没有“收口”。
3. `P2: 可靠性与补偿闭环`
   - 在真实交易上线前，需要把 MQ、重试、死信、补偿、纠偏串起来，否则局部失败会直接暴露成业务黑洞。
4. `P3: 退款业务域`
   - 退款属于第二条核心交易链路，但优先级应低于支付主链路闭环，因为退款通常复用支付单、支付状态和下游通道能力。
5. `P4: 通用交易查询与运营支撑`
   - 有必要，但应依附于前面几项真实业务能力完成后再扩展，避免先做“看得见的查询页”，却没有稳定的交易闭环作为真实数据来源。

## P0 Slice 1: Mock Dubbo Downstream Orchestration
- 当前 `BootstrapPaymentCreateApplicationService` 原先只是在路由命中后本地生成 `gatewayPaymentId` 并返回脚手架文案，没有真实下游调用，这与 P0 目标不符。
- 本轮已新增下游调用抽象 `DownstreamPaymentCreateGateway`，并在：
  - `default` profile 下提供 `InMemoryDownstreamPaymentCreateGateway`；
  - `local/docker` profile 下提供 `DubboDownstreamPaymentCreateGateway`。
- 为保持契约清晰，本轮新增 Dubbo 契约：
  - `com.example.payment.api.PaymentCreateFacade`
  - `PaymentCreateFacadeRequest`
  - `PaymentCreateFacadeResponse`
- 本地联调采用 `MockPaymentCreateFacadeImpl` 作为 Mock Dubbo 服务，符合 `AGENTS.dev-environment.md` 中“优先使用 Mock Dubbo 服务或测试桩服务”的建议。
- 应用层当前行为已变为：
  - 先完成安全校验、治理与幂等；
  - 再命中路由；
  - 再调用 Mock Dubbo 下游；
  - 将下游结果映射为外部 `PaymentCreateResponse`；
  - 并把 `downstreamPaymentId` 透传到 Outbox 载荷中。
- 当前仍然不是“真实生产支付服务”：
  - 下游仍是本仓库内的 Mock Dubbo Facade；
  - 还没有网关支付单专属持久化模型；
  - 还没有更细粒度的下游业务失败分类和正式协议映射。

## P0 Slice 1 Verification
- 测试验证通过：
  - `BootstrapPaymentCreateApplicationServiceTest`
  - `DubboDownstreamPaymentCreateGatewayTest`
  - `PaymentControllerTest`
  - `AcceptancePageControllerTest`
  - `PaymentGatewayApplicationTest`
  - 全仓 `mvn test`
- Docker 运行态验证通过：
  - `gateway-app` 重建成功，容器恢复 `healthy`
  - `GET /actuator/health` 返回 `UP`
  - Nacos 服务列表可见 `providers:com.example.payment.api.PaymentCreateFacade:1.0.0:`
  - `POST /api/v1/payments` 真实签名请求返回：
    - HTTP `200`
    - `code=SUCCESS`
    - `data.status=ACCEPTED`
    - `data.message=Payment request accepted by mock downstream facade`
- 数据库验证通过：
  - `gateway_request_log` 最新成功记录的 `target_service` 已为 `com.example.payment.api.PaymentCreateFacade`
  - `gateway_mq_outbox` 最新载荷已出现 `downstreamPaymentId`

## P0 Slice 2: Payment Order Persistence
- 当前 P0 的第二个真实缺口不是“还能不能调到 Mock Dubbo”，而是“成功受理后有没有形成网关支付单与下游流水的稳定关联模型”。
- 本轮已新增公共持久化抽象：
  - `PaymentOrderRecord`
  - `PaymentOrderRepository`
- 并在基础设施层补齐两类实现：
  - `InMemoryPaymentOrderRepository`
  - `JdbcPaymentOrderRepository`
- MyBatis 持久化模型已落地：
  - `GatewayPaymentOrderEntity`
  - `GatewayPaymentOrderMapper`
- 应用层行为已变化为：
  - 支付请求完成安全、治理、幂等、路由和下游调用后；
  - 在返回 `PaymentCreateResponse` 前，将 `gatewayPaymentId / merchantId / requestId / idempotencyKey / routeCode / targetService / downstreamPaymentId / paymentStatus / amount / currency` 一并保存到 `gateway_payment_order`。
- 数据库契约也已同步补齐：
  - `docker/mysql/init/01-schema.sql` 新增 `gateway_payment_order` 表；
  - 运行态 MySQL 已实际执行最新建表脚本，表已存在于 `gateway_db`。

## P0 Slice 2 Verification
- 失败测试先行：
  - `BootstrapPaymentCreateApplicationServiceTest` 先要求成功路径必须调用 `PaymentOrderRepository.save(...)`；
  - 并断言保存记录中的 `downstreamPaymentId= DSP-20260421-0001`、`routeCode=ROUTE_PAY_CREATE`。
- 实现后测试验证通过：
  - `.\mvnw.cmd --% -q -gs .mvn/global-settings.xml -s .mvn/settings.xml -pl gateway-application -am test -Dtest=BootstrapPaymentCreateApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
  - `.\mvnw.cmd --% -q -gs .mvn/global-settings.xml -s .mvn/settings.xml -pl gateway-app -am test -Dtest=PaymentGatewayApplicationTest -Dsurefire.failIfNoSpecifiedTests=false`
  - `.\mvnw.cmd --% -q -gs .mvn/global-settings.xml -s .mvn/settings.xml test -Dsurefire.failIfNoSpecifiedTests=false`
- Docker 运行态验证通过：
  - `.\mvnw.cmd --% -q -gs .mvn/global-settings.xml -s .mvn/settings.xml -pl gateway-app -am package -DskipTests` 成功；
  - `docker compose -f docker/local-compose.yml up -d --build gateway-app` 成功；
  - `gateway-app` 当前为 `healthy`，`GET /actuator/health` 返回 `UP`。
- 真实 HTTP 与数据库证据已经闭合：
  - 真实签名请求返回 HTTP `200`，`code=SUCCESS`，`data.status=ACCEPTED`；
  - `gateway_payment_order` 最新记录已出现：
    - `gateway_payment_id = GP177684620500233802`
    - `request_id = REQ-P0-1776846203785`
    - `route_code = ROUTE_PAY_CREATE`
    - `target_service = com.example.payment.api.PaymentCreateFacade`
    - `downstream_payment_id = DSP2222820126`
    - `payment_status = ACCEPTED`
  - `gateway_request_log` 最新同一请求也已记录 `target_service = com.example.payment.api.PaymentCreateFacade`；
  - `gateway_mq_outbox` 最新载荷与支付单记录中的 `gatewayPaymentId/downstreamPaymentId/requestId` 保持一致。

## Updated P0 Focus
- 经过 Slice 1 和 Slice 2 之后，P0 已不再只是“入口收到请求并打到 Mock Dubbo”，而是具备了：
  - 安全、治理、幂等；
  - 路由命中；
  - 下游编排；
  - 网关支付单与下游流水关联持久化；
  - Outbox 事件透传。
- 因此下一步不该重复扩表或增加更多占位接口，而应直接进入：
  - 真实支付服务契约替换；
  - 下游超时、拒绝、空响应、系统失败的错误分类；
  - 对失败语义的持久化与回放策略。

## P0 Slice 3: Downstream Failure Classification And Replay
- Slice 1 和 Slice 2 打通后，新的真实缺口变成了：下游虽然已经能返回 `ACCEPTED`，但当下游明确返回“拒绝”或“失败”时，网关还只会把它们当作普通成功返回或笼统服务异常，缺少清晰错误语义。
- 本轮新增的错误码是：
  - `DOWNSTREAM_REJECTED`
  - `DOWNSTREAM_FAILED`
- `DubboDownstreamPaymentCreateGateway` 当前状态映射规则已明确为：
  - `ACCEPTED`、`PROCESSING`：视为成功结果并继续返回；
  - `REJECTED`：抛出 `GatewayException(DOWNSTREAM_REJECTED, 422, ...)`；
  - `FAILED/FAIL/ERROR`：抛出 `GatewayException(DOWNSTREAM_FAILED, 502, ...)`；
  - `null/blank` 状态：继续归类为 `DOWNSTREAM_EMPTY_RESPONSE`；
  - 不支持的状态：归类为 `DOWNSTREAM_SERVICE_ERROR`。
- 这次没有扩大 Dubbo 接口契约，而是先利用已有 `status/message` 字段做最小分类，符合“强兼容边界先保守演进”的要求。

## Mock Dubbo Failure Harness
- 为了做真实运行态回归，本轮把 `MockPaymentCreateFacadeImpl` 从“永远 ACCEPTED”扩展为“按 requestId 前缀返回可控状态”的测试桩：
  - `REQ-REJECT*` -> `REJECTED`
  - `REQ-FAIL*` -> `FAILED`
  - 其他请求 -> `ACCEPTED`
- 这让本地 Docker 环境可以真实打出：
  - `422 DOWNSTREAM_REJECTED`
  - `502 DOWNSTREAM_FAILED`
  而不需要伪造控制器返回或跳过 Dubbo 调用。

## Failure Replay And Side-Effect Boundaries
- `PaymentCreateIdempotencyCoordinator` 原本就能缓存 `GatewayException` 失败结果；本轮通过新增测试和真实 HTTP 回归确认：
  - 同 `requestId + idempotencyKey + amount` 的失败请求，第二次会重放同一失败语义；
  - 不会被误转成成功；
  - 不会重新执行业务副作用。
- 应用层边界也已补强：
  - 下游拒绝/失败时，不写 `gateway_payment_order`；
  - 不发送 `gateway_mq_outbox`；
  - 但仍然记录请求日志与异常事件。

## Failure Observability Fix
- 运行态验收时还额外发现一个真实缺陷：失败请求虽然返回了正确错误码，但 `gateway_request_log.target_service` 为空，导致审计与故障排查缺失下游指向。
- 根因是 `BootstrapPaymentCreateApplicationService.persistRequestLog(...)` 原先只在成功响应场景回填路由信息。
- 现已修正为：
  - 仅当异常属于“后路由阶段失败”时，补充 `routeCode/targetService`；
  - 安全失败、限流失败等前置失败仍不伪造路由信息。
- 当前已验证失败请求日志中也会正确落：
  - `route_code = ROUTE_PAY_CREATE`
  - `target_service = com.example.payment.api.PaymentCreateFacade`

## P0 Slice 3 Verification
- 单元/模块测试通过：
  - `DubboDownstreamPaymentCreateGatewayTest`
  - `MockPaymentCreateFacadeImplTest`
  - `BootstrapPaymentCreateApplicationServiceTest`
  - `PaymentCreateIdempotencyCoordinatorTest`
  - `PaymentControllerTest`
  - 全仓 `mvn test`
- 真实 Docker 回归通过：
  - `gateway-app` 重建后保持 `healthy`
  - `GET /actuator/health` 返回 `UP`
  - `REQ-REJECT-20260422-0002` 第一次请求返回：
    - HTTP `422`
    - `code=DOWNSTREAM_REJECTED`
  - 同一 `requestId/idempotencyKey` 更换 `nonce/requestTime` 后再次请求，仍返回：
    - HTTP `422`
    - `code=DOWNSTREAM_REJECTED`
  - `REQ-FAIL-20260422-0002` 返回：
    - HTTP `502`
    - `code=DOWNSTREAM_FAILED`
- 数据库证据通过：
  - `gateway_request_log`：
    - 拒绝请求已落 `ROUTE_PAY_CREATE + com.example.payment.api.PaymentCreateFacade + DOWNSTREAM_REJECTED`
    - 失败请求已落 `ROUTE_PAY_CREATE + com.example.payment.api.PaymentCreateFacade + DOWNSTREAM_FAILED`
  - `gateway_exception_event`：
    - 拒绝请求为 `event_level = WARN`
    - 失败请求为 `event_level = ERROR`
  - `gateway_payment_order`：
    - 对 `REQ-REJECT-20260422-0002` 与 `REQ-FAIL-20260422-0002` 记录数均为 `0`
  - `gateway_mq_outbox`：
    - 对应失败请求记录数为 `0`

## P0 Slice 4: Formal Dubbo DTO Mapping And Processing Semantics
- 第四个切片的目标不是再扩新接口，而是把现有 Mock Dubbo 编排从“技术可用”推进到“协议更稳定、处理中语义明确”。
- 当前已新增 `PaymentCreateFacadeMapper`：
  - 使用 `MapStruct` 统一把 `GatewayRouteDefinition + gatewayPaymentId + DownstreamPaymentCreateRequest` 转成 `PaymentCreateFacadeRequest`；
  - 避免控制类内手写 DTO 拼装继续扩散。
- `MockPaymentCreateFacadeImpl` 现已支持：
  - `REQ-PROCESSING* -> PROCESSING`
  - `REQ-REJECT* -> REJECTED`
  - `REQ-FAIL* -> FAILED`
  - 其他请求 -> `ACCEPTED`
- `DubboDownstreamPaymentCreateGateway` 当前语义已稳定为：
  - `ACCEPTED/PROCESSING` 都视为成功类响应并继续回到上游；
  - `REJECTED` / `FAILED` 继续按第三切片既定错误码处理。

## Dubbo DTO Compatibility Findings
- 运行态首次回归时暴露出两个真实兼容性问题：
  - Dubbo 严格序列化白名单未显式包含 `PaymentCreateFacadeRequest/Response`；
  - Dubbo Hessian2 对 Java `record` DTO 反序列化失败，错误为“can't get field offset on a record class”。
- 结论很明确：
  - 当前这套 Dubbo/Hessian2 运行边界下，不应继续把跨 Dubbo 的 DTO 定义为 `record`；
  - 应改为普通可序列化 POJO，并保留稳定访问方法以减少调用侧改动。
- 因此本轮已落实：
  - `gateway-api` 中 `PaymentCreateFacadeRequest` 与 `PaymentCreateFacadeResponse` 改为普通 POJO；
  - 保留 record 风格访问方法，降低改动面；
  - 新增 `gateway-api/src/main/resources/security/serialize.allowlist`；
  - 新增 `gateway-infrastructure/src/test/resources/security/serialize.allowlist` 用于测试类路径加载。

## P0 Slice 4 Runtime Fixes
- `gateway-app` 最新镜像一度启动失败，报：
  - `DubboDownstreamPaymentCreateGateway` 无默认构造器。
- 根因不是 Dubbo 本身，而是该类同时存在：
  - 面向 Spring 的注入构造器；
  - 面向测试的包级构造器；
  导致 Spring 在运行态无法唯一选择。
- 最小修复是显式标注注入构造器，而不是重构测试入口或扩大 Bean 定义改动。
- 本轮还确认过一个构建层问题：
  - 并行执行共享模块 Maven 任务会竞争写 `gateway-infrastructure/target/generated-sources/annotations/PaymentCreateFacadeMapperImpl.java`；
  - 改回串行构建并清理生成目录后恢复正常。

## P0 Slice 4 Verification
- 定向测试通过：
  - `PaymentCreateFacadeDtoSerializationTest`
  - `DubboDownstreamPaymentCreateGatewayTest`
  - `MockPaymentCreateFacadeImplTest`
  - `BootstrapPaymentCreateApplicationServiceTest`
- 全仓验证通过：
  - `.\mvnw.cmd --% -q -gs .mvn/global-settings.xml -s .mvn/settings.xml test -Dsurefire.failIfNoSpecifiedTests=false`
- Docker 与真实 HTTP 回归通过：
  - `.\mvnw.cmd --% -q -gs .mvn/global-settings.xml -s .mvn/settings.xml -pl gateway-app -am package -Dmaven.test.skip=true`
  - `docker compose -f docker/local-compose.yml build --no-cache gateway-app`
  - `docker compose -f docker/local-compose.yml up -d gateway-app`
  - `gateway-app` 当前为 `healthy`
  - 真实签名请求：
    - `REQ-PROCESSING-20260422-0005 / IDEMP-PROCESSING-20260422-0005`
  - 返回结果：
    - HTTP `200`
    - `code=SUCCESS`
    - `data.status=PROCESSING`
    - `data.gatewayPaymentId=GP177684986012858369`
- 数据库证据已闭合：
  - `gateway_request_log`：
    - `REQ-PROCESSING-20260422-0005 / SUCCESS / ROUTE_PAY_CREATE / com.example.payment.api.PaymentCreateFacade`
  - `gateway_payment_order`：
    - `gateway_payment_id = GP177684986012858369`
    - `downstream_payment_id = DSP4049772361`
    - `payment_status = PROCESSING`
  - `gateway_mq_outbox`：
    - `event_key = OUTBOX-GP177684986012858369`
    - `send_status = 1`
    - `payload_json.requestId = REQ-PROCESSING-20260422-0005`
    - `payload_json.status = PROCESSING`

## P0 Slice 5: Downstream Timeout And Service Error Semantics
- 第五个切片聚焦的是第三、第四切片之外仍未闭合的技术失败语义：
  - `DOWNSTREAM_TIMEOUT`
  - `DOWNSTREAM_SERVICE_ERROR`
- 当前网关主逻辑本身已有这两个错误码分支，但此前缺少：
  - Mock Dubbo 业务桩的真实触发方式；
  - 控制器、应用层、幂等回放的专项测试；
  - 运行态真实 HTTP 与数据库证据。

## P0 Slice 5 Code Findings
- 已补齐测试覆盖：
  - `DubboDownstreamPaymentCreateGatewayTest`
    - `RpcException timeout -> DOWNSTREAM_TIMEOUT`
    - `RpcException -> DOWNSTREAM_SERVICE_ERROR`
  - `BootstrapPaymentCreateApplicationServiceTest`
    - 超时/服务异常时不落支付单、不发 Outbox，且失败日志保留路由信息
  - `PaymentCreateIdempotencyCoordinatorTest`
    - 已保存的 `DOWNSTREAM_TIMEOUT` 失败结果可被重复请求回放
  - `PaymentControllerTest`
    - `504 DOWNSTREAM_TIMEOUT`
    - `502 DOWNSTREAM_SERVICE_ERROR`
- `MockPaymentCreateFacadeImpl` 已新增：
  - `REQ-ERROR* -> 抛 RpcException("payment create failed in mock downstream facade")`
  - `REQ-TIMEOUT* -> 延迟 3500ms`，用于逼近消费者侧 `3000ms` 超时判定，而不是 provider 主动伪造 timeout 异常

## P0 Slice 5 Validation Findings
- 定向测试当前已通过：
  - `DubboDownstreamPaymentCreateGatewayTest`
  - `MockPaymentCreateFacadeImplTest`
  - `BootstrapPaymentCreateApplicationServiceTest`
  - `PaymentCreateIdempotencyCoordinatorTest`
  - `PaymentControllerTest`
- 打包与镜像构建已通过：
  - `gateway-app` Maven package 成功
  - `docker compose build gateway-app` 成功
- 运行态首次真实回归时发现一个关键差异：
  - provider 直接抛 `RpcException(TIMEOUT_EXCEPTION, ...)` 在真实 Dubbo 链路上并未稳定保留 timeout 语义；
  - 实际 HTTP 返回成了 `502 DOWNSTREAM_SERVICE_ERROR`；
  - 因此已把超时 Mock 从“主动抛超时异常”改为“慢响应超过 consumer timeout”。
- 但这次修正后的最终运行态验收尚未完成，原因不是代码，而是本机 Docker Desktop/WSL 引擎在重建过程中失稳。

## Docker Runtime Blocker Snapshot
- 目前已确认的环境现象：
  - `docker version` 无法连接 `//./pipe/dockerDesktopLinuxEngine`
  - 之前一度返回 Docker API `500 Internal Server Error`
  - `wsl -l -v` 显示 `docker-desktop` 为 `Running`
  - `Get-Service com.docker.service` 显示 `Stopped`
  - 中途还出现过 `docker-desktop` WSL 侧 `getpwuid(0) failed`
- 结论：
  - `P0` 第五切片的“代码和定向测试”已具备继续收口条件；
  - 当前阻塞点完全在本机 Docker 引擎恢复，而不在项目代码。

## P0 Slice 5 Runtime Closure
- 后续恢复过程中已确认：`com.docker.service` 恢复为 `Running` 后，`docker version`、`docker ps`、`docker compose -f docker/local-compose.yml ps gateway-app` 全部恢复正常，`gateway-app` 重新达到 `healthy`，`GET /actuator/health` 返回 `UP`。
- 恢复 Docker 后先做了真实回归，结果暴露出一个比“环境阻塞”更关键的运行时事实：
  - 旧容器里的 `MockPaymentCreateFacadeImpl` 仍是“provider 主动抛 timeout RpcException”的旧实现，真实请求 `REQ-TIMEOUT-20260422-0005` 被映射成了 `502 DOWNSTREAM_SERVICE_ERROR`；
  - 随后核对本地 `target/classes` 和打包后的嵌套 jar，确认源码和制品已经是新版本，问题只在于运行容器未完成最新替换。
- 使用 `docker compose build --no-cache gateway-app` + `docker compose up -d gateway-app` 重建后，又暴露出第二个更本质的问题：
  - 当前本地 Dubbo `injvm` 运行边界下，provider 端 `sleep 3500ms` 并不会稳定触发 consumer `3000ms` timeout；
  - 真实请求 `REQ-TIMEOUT-20260422-0006` 实际返回了 `200 SUCCESS / ACCEPTED`；
  - 结论是“慢响应逼近 consumer timeout”这一模拟策略在当前技术栈下不成立，不能继续依赖。
- 因此第五切片的最终修正改成了“consumer 侧 timeout 语义判定”：
  - `MockPaymentCreateFacadeImpl` 恢复为直接抛出 `RpcException("payment create timed out in mock downstream facade")`；
  - `DubboDownstreamPaymentCreateGateway` 新增 `isTimeoutException(...)`，统一识别：
    - `exception.isTimeout()`
    - 异常 message 含 `timed out` / `timeout`
    - 嵌套 cause message 含 `timed out` / `timeout`
  - 识别为超时后稳定映射为 `DOWNSTREAM_TIMEOUT / HTTP 504`；
  - 未命中超时语义的 `RpcException` 继续映射为 `DOWNSTREAM_SERVICE_ERROR / HTTP 502`。
- 这个修正符合当前项目的现实边界：不去伪造一个本地 `injvm` 实际不具备的 timeout 机制，而是在 consumer 侧用可验证、可回归的方式稳定表达超时语义。

## P0 Slice 5 Final Verification
- 定向测试最终通过：
  - `DubboDownstreamPaymentCreateGatewayTest`
  - `MockPaymentCreateFacadeImplTest`
  - `BootstrapPaymentCreateApplicationServiceTest`
  - `PaymentCreateIdempotencyCoordinatorTest`
- 全仓验证最终通过：
  - `.\mvnw.cmd --% -q -gs .mvn/global-settings.xml -s .mvn/settings.xml test -Dsurefire.failIfNoSpecifiedTests=false`
  - 期间仍可见 Testcontainers 对 Docker 环境探测的日志噪音，但 Maven 退出码为 `0`，不影响当前回归结论。
- 真实 HTTP 结果：
  - `REQ-TIMEOUT-20260422-0007 / IDEMP-TIMEOUT-20260422-0007`
    - 首次请求：HTTP `504`，`code=DOWNSTREAM_TIMEOUT`
    - 同 `requestId/idempotencyKey`、更新 `nonce/requestTime` 的重放请求：HTTP `504`，仍为 `code=DOWNSTREAM_TIMEOUT`
  - `REQ-ERROR-20260422-0007 / IDEMP-ERROR-20260422-0007`
    - 请求结果：HTTP `502`，`code=DOWNSTREAM_SERVICE_ERROR`
- 数据库证据：
  - `gateway_request_log`
    - `REQ-TIMEOUT-20260422-0007` 已落 `FAIL / DOWNSTREAM_TIMEOUT / ROUTE_PAY_CREATE / com.example.payment.api.PaymentCreateFacade`
    - `REQ-ERROR-20260422-0007` 已落 `FAIL / DOWNSTREAM_SERVICE_ERROR / ROUTE_PAY_CREATE / com.example.payment.api.PaymentCreateFacade`
  - `gateway_exception_event`
    - timeout 请求已落 `event_level=ERROR / event_type=DOWNSTREAM_TIMEOUT / event_code=DOWNSTREAM_TIMEOUT / event_message=Downstream payment create timed out`
    - error 请求已落 `event_level=ERROR / event_type=DOWNSTREAM_SERVICE_ERROR / event_code=DOWNSTREAM_SERVICE_ERROR / event_message=Downstream payment create invocation failed`
  - `gateway_payment_order`
    - `REQ-TIMEOUT-20260422-0007` 计数为 `0`
    - `REQ-ERROR-20260422-0007` 计数为 `0`
  - `gateway_mq_outbox`
    - timeout 与 error 请求计数均为 `0`
- 结论：
  - `P0` 第五切片已经完成；
  - 失败回放成立、失败日志保留路由信息、支付单与 Outbox 副作用边界成立；
  - 当前 `P0` 的前五个业务切片全部闭合，后续应转入真实支付服务契约替换。

## P1 Closure: Query, Callback And Status Transition
- `P1` 当前已具备三条业务能力：
  - `POST /api/v1/payments/query`
  - `POST /api/v1/payments/callback`
  - `gateway_payment_order.payment_status` 的合法状态流转
- 查询链路当前规则：
  - 对 `SUCCEEDED / FAILED / CLOSED / REJECTED` 等终态订单，优先读取本地支付单；
  - 对 `ACCEPTED / PROCESSING` 等非终态订单，通过 `ROUTE_PAY_QUERY -> PaymentQueryFacade` 主动补查；
  - 下游返回的新状态会回写 `gateway_payment_order`。
- 回调链路当前规则：
  - 验签、时间窗、防重放仍复用统一安全边界；
  - 必须匹配 `merchantId + gatewayPaymentId + downstreamPaymentId`；
  - 仅允许 `PaymentStatusTransitions` 里定义的合法迁移；
  - 成功后回写本地支付单状态。

## P1 Runtime Fixes
- 本轮最先暴露的并不是业务逻辑错误，而是 Docker 运行方式问题：
  - `gateway-app.jar` 在 Linux 容器里的 nested-jar 启动边界下，反复报 `GatewayException` 类加载失败；
  - 但宿主机直接 `java -jar gateway-app.jar` 又可正常启动。
- 结论是当前本仓库的运行制品更适合使用 Spring Boot exploded 方式，而不是继续把问题归咎为源码缺类。
- 因此 `gateway-app/Dockerfile` 已改为：
  - 先复制 `gateway-app.jar`
  - 再用 `java -Djarmode=tools -jar /app/app.jar extract`
  - 最后在 `/app/app` 下以 `java -jar app.jar` 启动
- 这样做后，运行态真实错误从“nested-jar 类加载异常”收敛成了真正的业务装配问题，便于继续修复。

## P1 Query Gateway Wiring Findings
- `DubboDownstreamPaymentQueryGateway` 首次进入真实容器时暴露了两个装配问题：
  - 未声明 `@Profile({\"local\", \"docker\"})`，导致与 `InMemoryDownstreamPaymentQueryGateway` 在某些运行边界下形成双实现歧义；
  - 同时存在生产构造器和测试构造器，Spring 在运行态报“无默认构造器”。
- 当前修复方式与 `DubboDownstreamPaymentCreateGateway` 保持一致：
  - 明确 `local/docker` profile；
  - 显式标注注入构造器。

## P1 Final Verification
- Docker 运行态：
  - `gateway-app` 当前 `healthy`
  - `GET /actuator/health` 返回 `UP`
  - Nacos 服务列表中已出现：
    - `providers:com.example.payment.api.PaymentCreateFacade:1.0.0:`
    - `providers:com.example.payment.api.PaymentQueryFacade:1.0.0:`
- 真实 HTTP 与状态闭环证据：
  - 场景 A：
    - 创建请求 `REQ-QUERY-20260423-0001` 返回 `200 SUCCESS / ACCEPTED`
    - 查询请求 `REQ-QUERY-CHECK-20260423-0001` 返回 `200 SUCCESS / SUCCEEDED`
  - 场景 B：
    - 创建请求 `REQ-PROCESSING-20260423-0008` 返回 `200 SUCCESS / PROCESSING`
    - 回调请求 `REQ-CALLBACK-20260423-0008` 返回 `200 SUCCESS / SUCCEEDED`
    - 回调后查询 `REQ-QUERY-AFTER-CALLBACK-20260423-0008` 返回 `200 SUCCESS / SUCCEEDED`
- MySQL 证据：
  - `gateway_payment_order`
    - `REQ-QUERY-20260423-0001` 已更新为 `SUCCEEDED`
    - `REQ-PROCESSING-20260423-0008` 已更新为 `SUCCEEDED`
  - `gateway_request_log`
    - 查询请求已落 `api_code=QUERY / route_code=ROUTE_PAY_QUERY / target_service=com.example.payment.api.PaymentQueryFacade / response_code=SUCCESS`
    - 回调请求已落 `api_code=CALLBACK / response_code=SUCCESS`
  - `gateway_exception_event`
    - 上述成功查询/回调请求当前无异常事件记录

## Next Priority After P1
- `P1` 已完成后，最直接的真实缺口变成了 `P2`：
  - 处理中订单如果长期未收到回调，当前仍缺主动补查/补偿任务；
  - 当前已存在 Outbox、MQ、审计、异常事件和查询链路，已经具备实现“恢复能力”而不是重新造基础设施的条件。
- 因此下一步建议优先做：
  - 补偿任务扫描 `PROCESSING` 订单；
  - 调用现有 Query 能力补查并纠偏；
  - 记录补偿事件、重试次数与最终结果。

## P2 Slice 1: Processing Order Reconcile
- 当前已新增补偿链路骨架：
  - `PaymentReconcileApplicationService`
  - `ProcessingPaymentReconcileApplicationService`
  - `ProcessingPaymentReconcileScheduler`
  - `PaymentReconcileController`
  - `GatewayReconcileProperties`
- `PaymentOrderRepository` 已补 `findByPaymentStatus(String paymentStatus, int limit)`，并在：
  - `InMemoryPaymentOrderRepository`
  - `JdbcPaymentOrderRepository`
  中都已实现。
- 补偿逻辑当前行为：
  - 扫描 `PROCESSING` 订单；
  - 复用现有 `ROUTE_PAY_QUERY -> DownstreamPaymentQueryGateway`；
  - 仅在 `PaymentStatusTransitions` 允许时回写支付单状态；
  - 所有补偿尝试都会落 `gateway_request_log`；
  - 查询失败会追加 `gateway_exception_event`。
- 当前补偿接口是内部技术入口：
  - `POST /api/v1/payments/reconcile/processing`
  - 不要求上游业务签名，定位是运维/脚手架验收入口，而不是对商户开放的新业务 API。

## P2 Slice 1 Verification
- 定向测试通过：
  - `ProcessingPaymentReconcileApplicationServiceTest`
  - `PaymentReconcileControllerTest`
  - `PaymentGatewayApplicationTest`
- 真实 Docker 验证通过：
  - `gateway-app` 重建后 `GET /actuator/health` 返回 `UP`
  - 已通过真实签名请求创建 3 条验收支付单，并在 MySQL 中将它们改写成可控的 `PROCESSING` 场景：
    - `REQ-RECON-UPDATE-20260423-0001`：查询应回 `SUCCEEDED`
    - `REQ-PROCESSING-RECON-20260423-0001`：查询应回 `PROCESSING`
    - `REQ-ERROR-RECON-20260423-0001`：查询应抛下游异常
- 真实运行态结论：
  - 定时任务已经在 Docker 中生效，并先一步把 `GP177688639635910264` 从 `PROCESSING` 纠偏为 `SUCCEEDED`
  - 手工 `POST /api/v1/payments/reconcile/processing` 返回：
    - `scannedCount=3`
    - `updatedCount=0`
    - `unchangedCount=2`
    - `failedCount=1`
  - 其中 `updatedCount=0` 不是逻辑错误，而是因为该次手工请求处理的是“调度器已扫过之后的剩余集合”
- MySQL 证据：
  - `gateway_payment_order`
    - `GP177688639635910264 -> SUCCEEDED`
    - `GP177688639646410265 -> PROCESSING`
    - `GP177688639653910266 -> PROCESSING`
  - `gateway_request_log`
    - 已存在 `api_code=RECONCILE` 的 `SUCCESS / status=SUCCEEDED`
    - 已存在 `api_code=RECONCILE` 的 `SUCCESS / status=PROCESSING`
    - 已存在 `api_code=RECONCILE` 的 `DOWNSTREAM_SERVICE_ERROR`
  - `gateway_exception_event`
    - 已存在 `api_code=RECONCILE / event_type=DOWNSTREAM_SERVICE_ERROR / event_code=DOWNSTREAM_SERVICE_ERROR`

## P2 Slice 2: Outbox Retry And Manual Replay
- 当前已新增消息补偿链路：
  - `PaymentOutboxRetryApplicationService`
  - `PaymentOutboxRetryApplicationServiceImpl`
  - `PaymentOutboxRetryScheduler`
  - `PaymentOutboxRetryController`
  - `GatewayOutboxRetryProperties`
- 公共抽象已补齐：
  - `PaymentMqOutboxRecord`
  - `PaymentMqOutboxRepository`
  - `PaymentOutboxRetryExecutor`
- 当前行为：
  - 查询 `send_status=2` 且 `next_retry_time <= now` 的失败 Outbox；
  - 手工接口：`POST /api/v1/messaging/outbox/retry`
  - 调度器：按 `gateway.messaging.rocketmq.retry-cron` 周期执行；
  - 成功重放后：
    - `send_status -> 1`
    - `retry_count + 1`
    - `next_retry_time = NULL`
    - `last_error_message = NULL`
  - 重放失败后：
    - 保持 `send_status = 2`
    - `retry_count + 1`
    - 刷新 `next_retry_time`
    - 更新 `last_error_message`
  - 所有重放尝试都会写：
    - `gateway_request_log (api_code=RETRY)`
    - `gateway_exception_event (仅失败时)`

## P2 Slice 2 Runtime Findings
- 运行态首次 `POST /api/v1/messaging/outbox/retry` 返回 `500`，根因不是控制器逻辑，而是：
  - 新增 `PaymentOutboxRetryController` 已进入源码和宿主机产物；
  - 但旧 `gateway-web` 仍在容器里运行；
  - 表现为 Spring 返回 `No static resource api/v1/messaging/outbox/retry`
- 这次问题最终通过以下方式收敛：
  - `clean package`
  - `docker compose build --no-cache gateway-app`
  - 显式删除旧 `gateway-app` 容器再 `up -d`
  - 并通过从容器内 `docker cp` 回宿主机后反编译字节码，确认新控制器和新应用服务已真正进入运行镜像
- 运行态第二个真实问题是：
  - Outbox 重放成功后，`next_retry_time / last_error_message` 没被清空；
  - 根因是 `updateById` 与 `set(null)` 在当前 MyBatis-Plus 行为下没有按预期清空列；
  - 现已改为：
    - `LambdaUpdateWrapper`
    - `setSql("next_retry_time = NULL")`
    - `setSql("last_error_message = NULL")`

## P2 Slice 2 Verification
- 定向测试通过：
  - `PaymentOutboxRetryApplicationServiceTest`
  - `PaymentOutboxRetryControllerTest`
  - `PaymentGatewayApplicationTest`
- 真实 Docker / RocketMQ / MySQL 验收通过：
  - 插入 2 条失败 Outbox：
    - `OUTBOX-RETRY-SUCCESS-20260423-0001`
    - `OUTBOX-RETRY-FAIL-20260423-0001`
  - `POST /api/v1/messaging/outbox/retry` 返回：
    - `scannedCount=2`
    - `succeededCount=1`
    - `failedCount=1`
    - `retriedMessageKeys=["MSG-RETRY-SUCCESS-20260423-0001"]`
  - `gateway_request_log`
    - 已落 `api_code=RETRY / response_code=SUCCESS`
    - 已落 `api_code=RETRY / response_code=OUTBOX_RETRY_FAILED`
  - `gateway_exception_event`
    - 已落 `api_code=RETRY / event_type=OUTBOX_RETRY_FAILED`
- 字段清空修复后的最终证据：
  - 新增 `OUTBOX-RETRY-SUCCESS-20260423-0004`
  - 重放后 MySQL 中该行已为：
    - `send_status=1`
    - `retry_count=1`
    - `next_retry_time=NULL`
    - `last_error_message=NULL`

## P2 Slice 3: Notification Consumer Runtime Fix
- 当前 RocketMQ 通知消费侧的真实运行边界已经验证清楚：
  - 发送侧 `RocketMqPaymentOutboxPublisher` 发布的是 JSON 字符串 payload，并通过 header `KEYS` 透传 `gatewayPaymentId`。
  - 旧版 `PaymentEventConsumer` 使用 `RocketMQListener<Message<String>>`，在 Docker 运行态会让 Spring/RocketMQ 先尝试把 JSON 对象反序列化为 `String` payload，结果触发 `MessageConversionException`，表现为日志中的 `convert failed`。
- 最小修复不应改消息协议，也不应改 `PaymentNotificationProcessor` 业务处理器，而应收缩在消费边界：
  - 将消费签名改为 `RocketMQListener<MessageExt>`；
  - 直接从 `MessageExt#getKeys()` 读取 messageKey；
  - 直接从 `MessageExt#getBody()` 按 UTF-8 读取 payload。
- 这条修复符合当前架构护栏：
  - 不改领域模型；
  - 不改持久化契约；
  - 不改 Outbox 生产协议；
  - 仅修正 MQ 适配层的类型边界。

## P2 Slice 3 Verification
- 新增定向测试：
  - `PaymentEventConsumerTest`
    - 正常透传 `messageKey/payload`
    - 无 keys 时生成 `UNKNOWN-*`
    - 普通失败抛异常触发重试
    - dead-letter 失败不再向上抛
- 定向测试通过：
  - `PaymentEventConsumerTest`
  - `PaymentNotificationRetryApplicationServiceTest`
  - `PaymentGatewayApplicationTest`
- 真实 Docker 验证通过：
  - 重建 `gateway-app` 后，`docker logs gateway-app` 当前已不再出现 `convert failed / cannot convert message / MessageConversionException`
  - `gateway-app` 当前为 `healthy`
  - `GET /actuator/health` 返回 `UP`
  - `POST /api/v1/messaging/notifications/retry` 返回 `200 / SUCCESS`
- 真实 MQ 消费证据：
  - 使用真实签名请求 `REQ-CONSUME-20260423-012304-2` 触发支付创建与 Outbox 发送
  - MySQL `gateway_message_consume_record` 最新记录：
    - `message_key = GP177690738505616849`
    - `consume_status = SUCCESS`
    - `retry_count = 0`
    - `dead_letter = 0`
  - MySQL `gateway_request_log` 最新消费记录：
    - `request_id = CONSUME-GP177690738505616849`
    - `api_code = CONSUME`
    - `response_code = SUCCESS`
    - `response_status = SUCCESS`
  - `gateway_exception_event` 当前无新的 `api_code=CONSUME` 失败事件

## Acceptance Page Drift Root Cause And Closure
- During stricter acceptance automation, a new runtime drift issue was confirmed:
  - workspace `gateway-app/src/main/resources/static/acceptance/index.html` was already new
  - `target/classes` and fat jar were also new
  - but container `/app/app/app.jar` and HTTP `/acceptance/index.html` still served the old page
- Fast markers:
  - old page: `--bg: #f3efe4`
  - new page: `--bg: #f2ede0`
- This proved the drift was not in Spring MVC static mapping.
- The real problem was Docker build determinism around `gateway-app` runtime contents.

## Docker Build Determinism Fix
- `gateway-app/Dockerfile` now uses a multi-stage source build:
  - builder copies repository source
  - builder runs `mvn --batch-mode -q -gs .mvn/global-settings.xml -s .mvn/settings.xml -pl gateway-app -am package -Dmaven.test.skip=true`
  - builder extracts the boot jar with `jarmode=tools`
  - runtime copies only exploded output
- `.dockerignore` no longer re-includes `gateway-app/target`.
- One builder-side issue was also closed:
  - using `./mvnw` inside `maven:3.9.11-eclipse-temurin-17` caused `/root/.m2` to be interpreted as a lifecycle phase
  - switching to image-native `mvn` resolved it

## Final Verification Evidence
- Build and recreate:
  - `docker compose -f docker/local-compose.yml build --no-cache gateway-app`
  - `docker compose -f docker/local-compose.yml up -d --force-recreate gateway-app`
- Final state:
  - `gateway-app` is `healthy`
  - HTTP `/acceptance/index.html` returns the new page
  - extracting container `app.jar` confirms:
    - `支付查询闭环`
    - `支付回调闭环`
    - `退款查询闭环`
    - `通知重试`
    - `支付超时注入`
    - `--bg: #f2ede0`

## P3 / P4 Runtime Reachability Snapshot
- `P3` 退款域当前在 Docker 运行态已可见：
  - `/api/v1/refunds` 返回参数校验错误而非 `404/NoResourceFound`
  - Nacos / Dubbo 注册中已出现 `com.example.payment.api.RefundFacade`
- `P4` 通用查询与审计接口当前在 Docker 运行态已可见：
  - `POST /api/v1/transactions/detail` 返回业务语义 `PAYMENT_ORDER_NOT_FOUND`
  - `GET /api/v1/transactions/audit?merchantId=MCH100001&requestId=REQ-NOT-FOUND` 返回 `SUCCESS` 且空集合
- 这说明此前“新控制器未进入运行镜像”的问题，在本轮最新镜像上已不再存在。

## Phase 25: Stateful Local Downstream Contract Replacement
- 支付下游当前不再只是“requestId 前缀 -> 固定状态”的无状态技术桩。
- `MockDownstreamPaymentStore` 已把 provider 侧最小契约状态固定下来：
  - create 以 `requestId/gatewayPaymentId/downstreamPaymentId` 三个维度索引 provider 记录；
  - query 优先从共享 store 读取，而不是只依赖当前查询请求的前缀；
  - 这让 `create -> query` 真正共享同一份 provider 状态。
- 当前支付 contract 的行为边界：
  - accepted create -> query maps to `SUCCEEDED`
  - processing create -> query keeps `PROCESSING`
  - timeout / error prefix injection 仍保留，避免既有异常验收退化
  - 未命中 store 的 query 仍可回退到原前缀规则，兼容旧验收入口
- 退款下游也已做同类收敛：
  - `MockDownstreamRefundStore`
  - `MockRefundFacadeImpl` create/query 共享 provider 状态
  - accepted refund create -> query maps to `SUCCEEDED`
  - processing refund create -> query keeps `PROCESSING`

## Phase 25 Verification
- Infra 定向测试通过：
  - `MockPaymentCreateFacadeImplTest`
  - `MockPaymentQueryFacadeImplTest`
  - `MockRefundFacadeImplTest`
  - `DubboDownstreamPaymentCreateGatewayTest`
  - `DubboDownstreamPaymentQueryGatewayTest`
- 应用 / 启动验证通过：
  - `BootstrapPaymentCreateApplicationServiceTest`
  - `BootstrapPaymentQueryApplicationServiceTest`
  - `PaymentGatewayApplicationTest`
- 真实 Docker 运行态验证通过：
  - `gateway-app` 重建后为 `healthy`
  - `GET /actuator/health` 返回 `UP`
  - 真实 HTTP 支付闭环：
    - create: `ACCEPTED`
    - query: `SUCCEEDED`
  - 真实 HTTP 支付处理中闭环：
    - create: `PROCESSING`
    - query: `PROCESSING`
  - 真实 HTTP 退款闭环：
    - payment create: `ACCEPTED`
    - refund create: `ACCEPTED`
    - refund query: `SUCCEEDED`

## Updated Next Focus
- 当前最大的缺口已不再是本地下游 contract 是否太“假”，而是：
  - 何时接入真正的外部 provider contract
  - 如何对 Redis/MySQL/RocketMQ/Seata 组合场景做更严格的异常注入验收
- 因此下一阶段建议：
  - 保持业务面稳定
  - 先做真实外部下游契约接入
  - 再做跨组件组合故障的自动化验收，而不是继续扩新 API

## Developer Validation Workbench Proposal
- The best developer-facing validation scheme is not a new page family.
- The most pragmatic option is to evolve the existing `/acceptance/index.html` into a single “developer validation workbench”.

### Why This Direction
- current page already has:
  - payment create / query / callback
  - refund create / query / callback
  - audit / transaction lookup
  - retry / reconcile
  - reject / timeout / downstream error injection
- developers already have a stable local/docker runtime and known entry URL
- adding a second validation UI would split usage and duplicate scenario logic

### Recommended Page Structure
1. Environment status
   - app health
   - middleware quick status
   - links to Nacos / SkyWalking / Prometheus
2. Single-feature validation
   - create / query / callback / refund / retries
   - editable request presets
3. Flow validation
   - payment closure
   - refund closure
   - reliability / retry / reconcile suites
4. Evidence and diagnostics
   - latest ids
   - latest request / response
   - copyable curl
   - diagnostic jump links

### What Makes It Convenient For Developers
- one page, no need to handcraft signatures or timestamps
- presets plus editable fields, so both smoke checks and targeted debug runs are supported
- latest context panel reduces repeated manual copying of payment / refund ids
- built-in evidence export lets devs carry request / response and IDs into logs, SQL checks, or bug reports

### Suggested Capability Tiers
- Tier 1:
  - preserve current one-click buttons and result panel
- Tier 2:
  - add form-driven preset editor above each action group
- Tier 3:
  - add step runner with visible progress for suites
- Tier 4:
  - add evidence export and diagnostic links
- Tier 5:
  - add reliability suite for Redis/MySQL/RocketMQ/Seata combined validation after real external contract replacement

## Developer Validation Workbench Implementation Closure
- The proposal has now been implemented on the existing `/acceptance/index.html`.
- Confirmed page markers in source and resource test:
  - `开发验证工作台`
  - `环境与诊断`
  - `场景预置`
  - `运行可靠性验收`
  - `复制最新 curl`
  - `最近证据`
- The implemented workbench keeps the original acceptance semantics but makes developer validation cheaper:
  - preset-driven input editing avoids handcrafting request ids and amounts
  - suite state exposes current run progress
  - evidence export reduces repeated copying from browser devtools
- `docs/acceptance-ui-guide.md` is now aligned to the workbench structure, so the page and the manual guide no longer describe different operator workflows.
- Runtime closure is also confirmed:
  - `gateway-app` rebuilt in Docker and returned to `healthy`
  - HTTP `/acceptance/index.html` snapshot contains the same workbench markers as workspace source

## File Placement Audit
- The repository's module layout is broadly aligned with `AGENTS.repository-layout.md`.
- No planning-file drift was found:
  - `.agent/` exists
  - legacy root-level `agent/`, `task_plan.md`, `findings.md`, `progress.md` are absent
- The main structure is not the problem:
  - `gateway-*` modules, `docs/`, and `docker/` all sit in expected top-level positions
- The current disorder is concentrated in root-level temporary residue created during runtime inspection and jar extraction:
  - `.tmpcmp/`
  - `.tmpextract/`
  - `.tmpinspect/`
  - `.tmpinspect2/`
  - `.tmpinspect3/`
  - `.tmpinspect4/`
  - `.tmpjar/`
  - `.tmp-host-docker.err`
  - `.tmp-host-docker.out`
  - `BOOT-INF/`
  - `com/`
- `BOOT-INF/` and `com/` at repo root are especially strong signals of an extracted Spring Boot jar landing in the wrong place, not intentional source layout.

## Residue Cleanup Safety Review
- Cleanup targets were reviewed before deletion, not removed blindly.
- Safety evidence:
  - `git ls-files` returned nothing for all residue paths
  - code search found no live references outside the `.agent` audit notes
  - `BOOT-INF/lib/*.jar` and `com/example/...class` confirmed extracted runtime artifacts, not source assets
- Cleanup was executed only after verifying each resolved absolute path stayed inside the repository root.
- Result:
  - all audited residue paths have been removed
  - current root now reflects the intended repository structure rather than mixed-in runtime inspection debris

## Preventive Rule Added
- A preventive rule has been added to `docs/agents/AGENTS.dev-environment.md`.
- It explicitly forbids leaving jar extraction, `docker cp`, decompile output, runtime snapshots, or temporary logs in the repo root or formal source directories.
- Temporary troubleshooting artifacts must now stay in `.tmp*/` or system temp and be cleaned before the task is closed.

## Delivery-Stage Documentation Added
- `docs/acceptance-operation-guide.md` was added as the operator-oriented companion to `docs/acceptance-ui-guide.md`.
- Its purpose is narrower than the full acceptance guide:
  - where to click first
  - which preset to choose
  - recommended smoke / regression / reliability flows
  - where to look first when a step fails
- `docs/final-delivery-open-items.md` was added to separate:
  - what is already closed for local sandbox acceptance
  - what is still missing before real user-facing final delivery
- The remaining gap is now documented as a prioritized delivery checklist rather than being scattered only in `.agent` notes.

## Final Delivery Checklist Findings (2026-04-24)
- `docs/final-delivery-open-items.md` 里的第一个真实问题并不只是“没有真实 provider 地址”，还有仓库内部的硬绑定：
  - sandbox provider 总是启用
  - Dubbo consumer 强制 `injvm=true`
  - 这意味着即便有真实 provider，仓库也会优先走本地 mock
- 这类仓库内阻塞现在已经清掉：
  - sandbox bean 已受 `gateway.downstream.sandbox.enabled` 控制
  - Dubbo consumer 已显式改为非 injvm 模式
- 商户安全配置此前过度耦合在 `GatewaySecurityProperties.merchants` 的原始结构上。
- 一个有价值且不扭曲现有契约的改进，是增加统一 provider 抽象，而不是凭空设计新的密钥协议：
  - `MerchantCredential`
  - `MerchantCredentialProvider`
  - `PropertiesMerchantCredentialProvider`
- 这样做的收益是：
  - 当前请求签名协议不变
  - 后续切换到 Nacos / MySQL / 混合模式时，不需要重写 validator 主链路
- `gateway-security.json` 已进入 local/docker profile 的 Nacos 导入链路，这比继续把商户安全配置固化在 `application.yml` 更贴近 AGENTS 文档要求。
- `docs/delivery/` 现已形成一组交付级材料骨架；因此“未完成项”已经不再包含“完全没有交付文档”这一类纯仓库内缺口。
- 现在剩余事项更加集中：
  - 真实外部 provider 契约与可用环境
  - 真实商户密钥托管与轮换规则
  - 正式联调环境证据
  - Redis/MySQL/RocketMQ/Seata 组合故障结论
- 本地可靠性验收已经从“手工探索”前移为“可复跑脚本”：
  - `scripts/local-reliability-suite.ps1`
  - 当前仓库 PowerShell 会话下已跑通
- 最新本地组件故障结论：
  - `redis-outage`：支付创建快速失败，当前映射为 `500 / INTERNAL_ERROR`
  - `mysql-outage`：支付创建快速失败，当前映射为 `500 / INTERNAL_ERROR`
  - `rocketmq-broker-outage`：支付主单仍可受理，Outbox 落失败态；Broker 恢复后将失败记录置为到期并调用重试接口，可恢复为已发送
  - `seata-outage`：支付创建当前仍成功并落单，这不是脚本 bug，而是现有运行语义，需要最终事务策略确认
- 因此，P1 的剩余问题不再是“完全没有自动化探针”，而是：
  - 是否把当前 Redis/MySQL 的 `500 INTERNAL_ERROR` 细化成更明确的稳定错误语义
  - 是否接受 Seata 掉线时仍继续受理的行为
  - 是否补齐真正的跨组件同时故障组合，而不只是单组件探针

## Delivery Optimization Findings (2026-04-29)
- Redis/MySQL 的泛化 `500 INTERNAL_ERROR` 已收敛：
  - Redis 故障现在返回 `503 / REDIS_UNAVAILABLE`
  - MySQL 故障现在返回 `503 / DATABASE_UNAVAILABLE`
  - Redis + MySQL 同时故障现在返回稳定 `503` 基础设施不可用语义，本轮实际结果为 `REDIS_UNAVAILABLE`
- `GatewayExceptionHandler` 采用 cause chain 分类，不让 Web 层直接依赖 Redis/JDBC 具体实现包：
  - class/message 命中 `redis/redisson/lettuce` -> `REDIS_UNAVAILABLE`
  - class/message 命中 `jdbc/mysql/sqlnontransientconnection/datasource/database` -> `DATABASE_UNAVAILABLE`
  - class/message 命中 `seata/transaction coordinator/global transaction` -> `TRANSACTION_COORDINATOR_UNAVAILABLE`
- 可靠性套件首次新增真正的本地组合场景 `redis-mysql-outage`，不再只覆盖单组件探针。
- Redisson 在 Redis 容器重启后可能短时间保持旧连接导致 Actuator `redis` health 仍为 `DOWN`；脚本现已增加 `gateway-app` 健康恢复兜底，必要时自动重启应用再继续后续场景。
- 最新证据目录 `.tmp-reliability/20260429-233650`：
  - `redis-outage => PASS`
  - `mysql-outage => PASS`
  - `redis-mysql-outage => PASS`
  - `rocketmq-broker-outage => PASS`
  - `seata-outage => REVIEW`
- Seata 仍然是正式交付前的真实决策项：
  - 当前本地行为为 Seata 掉线时支付创建仍返回 `200 / SUCCESS`
  - 这不是脚本缺陷，需明确最终事务策略后才能把 `REVIEW` 改为 `PASS` 或 `FAIL`
## Final Delivery Boundary Findings (2026-04-29)

- Repository-controlled delivery work now has an explicit handoff checklist in `docs/delivery/final-delivery-decision-checklist.md`.
- The remaining blockers are external inputs rather than local implementation gaps: real Dubbo provider contract, merchant key custody, final payment state policy, refund model decision, Seata outage policy, and target-environment evidence.
- Formal user acceptance should not be claimed from sandbox-only evidence; local Docker evidence proves technical readiness and regression safety, not real provider compatibility.
- The local reliability suite currently proves Redis, MySQL, Redis+MySQL, and RocketMQ broker behavior; Seata remains `REVIEW` until the transaction acceptance policy is decided.
