# Task Plan

## Goal
在 `payment-gateway` 仓库中，严格遵循 `AGENTS.md` 的分层、架构和安全约束，先完成剥离业务的最小技术脚手架：可运行、可测试、可验证，再以此作为后续业务特性开发基线。

## Current State
- `gateway-*` Maven 多模块骨架、应用入口、技术状态接口、静态验收页和支付创建技术受理入口均已落地。
- Docker 本地依赖编排已切换到当前源码对应的运行态，`gateway-app` 与 MySQL / Redis / Nacos / RocketMQ / Seata / SkyWalking / Sentinel / Prometheus / Grafana / ELK 均已通过实际连通性验收。
- 支付创建主链路已接入安全校验、幂等与并发控制、MySQL 持久化、路由查询、治理限流、审计查询与 Outbox 发送。
- 当前支付/退款 Dubbo 下游已从纯前缀规则 Mock 收敛为“共享 provider 状态的本地 sandbox 契约”，真实 HTTP 已验证支付与退款均具备 `create -> query` 闭环。
- `P2` 的 RocketMQ 通知消费、失败重试和死信边界已进入真实运行态；消费侧已按原生 `MessageExt` 收敛，不再触发框架级 `MessageConversionException`。
- `P3` 退款域与 `P4` 统一交易查询 / 审计增强代码、测试与最小 Docker 探针均已闭合，相关控制器在当前运行镜像内可直接访问。
- 技术脚手架阶段目标已完成；下一阶段属于“在稳定技术基线之上继续接入真实业务能力”。

## Phases
| Phase | Status | Notes |
|---|---|---|
| 1. 对齐 AGENTS 约束与现状 | completed | 已核对 project overview、build/test、dev env、coding conventions、architecture guardrails、docker compose、README |
| 2. 设计首批开发骨架与模块边界 | completed | 已明确父 POM、模块清单、最小启动链路与配置边界 |
| 3. 落 Maven 多模块与基础代码骨架 | completed | 已建立多模块 POM、应用入口、统一响应/异常、支付入口骨架、静态验收页 |
| 4. 补最小测试与启动验证 | completed | 已完成最小 Web 测试、应用上下文测试、全仓跳测打包验证 |
| 5. 记录结果与下一阶段开发入口 | completed | 已补 `.agent` 记录，并标记下一阶段重点为安全/幂等/路由/持久化接入 |
| 6. 运行态重建与组件连通验收 | completed | 已修复 Docker 构建链路与 Nacos/Sentinel 兼容项，`/api/v1/tech/status` 全量组件返回 UP |
| 7. 业务前基础能力优先级规划 | completed | 已按 AGENTS、README、验收指引整理待完成功能的分层优先级，明确先做安全入口链路，再做幂等/持久化/路由 |
| 8. 安全入口链路首个实现切片 | completed | 已完成 merchant 配置读取、验签、时间窗校验、防重放，并通过单元测试、应用测试和真实 HTTP 回归 |
| 9. 幂等与并发控制首个实现切片 | completed | 已完成幂等指纹、Redis 幂等记录、Redisson 锁、冲突返回码、重启后真实 HTTP 回归，并确认当前仍保持脚手架边界 |
| 10. 持久化/路由/治理/Outbox 技术切片 | completed | 已完成 MySQL 请求日志/异常事件/幂等落库、路由查询、治理配置与限流接口、RocketMQ Outbox 发送 |
| 11. 运行态回归与缺陷修正 | completed | 已完成全仓测试、Docker 重建、真实 HTTP 回归，并修复 `gateway_idempotency_record.request_hash` 字段长度不足导致的 `500 INTERNAL_ERROR` |
| 12. 文档与进度落盘收口 | completed | 已同步 README、bootstrap 状态描述与 `.agent` 文件，消除代码/运行态/文档漂移 |
| 13. P0 首个业务切片：Mock Dubbo 下游编排 | completed | 已完成支付创建到本地 Mock Dubbo Facade 的真实调用、响应映射、Outbox 下游流水透传、Docker 运行态回归 |
| 14. P0 第二个业务切片：支付单与下游流水关联持久化 | completed | 已新增 `gateway_payment_order` 模型与 MySQL 落库，真实请求已验证 `gatewayPaymentId/downstreamPaymentId/routeCode/targetService` 关联一致 |
| 15. P0 第三个业务切片：下游失败分类与失败语义回放 | completed | 已补齐 `DOWNSTREAM_REJECTED/DOWNSTREAM_FAILED`，真实 HTTP 验证 422/502 与幂等失败回放成立，且失败请求不落支付单/Outbox |
| 16. P0 第四个业务切片：正式 DTO 映射与 PROCESSING 语义 | completed | 已引入 MapStruct 请求映射、补齐 Dubbo DTO Hessian2 兼容与序列化白名单，真实 HTTP 验证 `PROCESSING` 返回、支付单落库与 Outbox 透传成立 |
| 17. P0 第五个业务切片：下游超时与系统异常语义 | completed | 已完成超时/服务异常测试、Mock Dubbo 行为修正、Docker 重建与真实 HTTP/数据库验收，`504 DOWNSTREAM_TIMEOUT` 与 `502 DOWNSTREAM_SERVICE_ERROR` 均已闭合 |
| 18. P1 支付结果闭环：查询 / 回调 / 状态流转 | completed | 已完成支付查询、异步回调、状态流转与真实 Docker 验收；`ACCEPTED -> SUCCEEDED` 主动查询闭环和 `PROCESSING -> SUCCEEDED` 回调闭环均已成立 |
| 19. P2 第一个业务切片：处理中订单补查 / 补偿任务 | completed | 已完成 `PROCESSING` 订单批量补查、调度器、手工补偿接口与真实 Docker / MySQL 验收；调度器可自动纠偏，手工接口可处理剩余处理中与失败订单 |
| 20. P2 第二个业务切片：Outbox 失败重试 / 手工重放 | completed | 已完成失败 Outbox 查询、手工重放接口、定时任务、RocketMQ 重试、请求日志 / 异常事件落库与真实 Docker / MySQL 验收 |
| 21. P2 第三个业务切片：通知消费 / 重试 / 死信边界 | completed | 已修复 `PaymentEventConsumer` 的消息转换边界，`/api/v1/messaging/notifications/retry` 可用，真实支付请求已验证 `gateway_message_consume_record / gateway_request_log(api_code=CONSUME)` 成功落库 |
| 22. P3 退款业务域：创建 / 查询 / 回调基线 | completed | 已完成退款创建、查询、回调、补偿基础代码与测试，Docker 运行态 `/api/v1/refunds*` 控制器已可达，`RefundFacade` 已在 Nacos / Dubbo 运行态注册 |
| 23. P4 通用交易查询与运营审计增强 | completed | 已完成统一交易查询、审计检索增强代码与测试，Docker 运行态 `/api/v1/transactions/detail` 与 `/api/v1/transactions/audit` 已可访问 |
| 24. 更严格业务级验收自动化与 Docker 构建确定性 | completed | 已完成新版 acceptance 页、资源测试、源码到容器运行页一致性修复、多阶段源码构建 |
| 25. 真实下游服务契约替换首个切片：本地 stateful sandbox contract | completed | 已完成支付/退款 provider 侧共享状态契约，真实 Docker 验证支付与退款的 `ACCEPTED -> SUCCEEDED`、支付 `PROCESSING -> PROCESSING` 查询链路成立 |

## Constraints
- 不绕过 `gateway-security`、`gateway-governance`、过滤器/拦截器、事务/锁/幂等等高风险语义。
- 不发起真实外部网络调用，不连接非本地 Docker 隔离环境。
- 所有敏感配置通过样例或环境变量表达，不提交真实密钥、密码、证书。
- 优先小步、可编译、可验证，不做无关重构。

## Business Phase Priorities
1. P0: 真实支付创建编排
   目标：把当前 `ACCEPTED` 技术受理占位替换为真实的下游支付创建调用、协议转换、路由命中、错误分类与响应映射。
   范围：
   - 外部支付创建 DTO 到内部 Dubbo/领域请求的正式转换；
   - 基于商户、业务类型、接口编码的真实路由选择；
   - 下游超时、失败、拒绝、处理中等状态的统一错误码映射；
   - 持久化网关单与下游流水关联信息，而不再只停留在技术占位结果。
2. P1: 支付结果闭环
   目标：补齐“创建后如何得到最终结果”的业务闭环，让网关不仅能受理，还能推进和暴露交易状态。
   范围：
   - 异步回调接入、验签、防重放与状态更新；
   - 支付状态流转模型与状态持久化；
   - 面向上游的支付结果查询接口；
   - 回调与主动查询的结果一致性约束。
3. P2: 可靠性与补偿闭环
   目标：让支付主链路在局部失败、MQ 异常、下游超时等情况下仍可恢复、可追踪、可补偿。
   范围：
   - RocketMQ 消费侧或通知侧正式接入；
   - 回调失败重试、死信、补偿任务与人工可诊断入口；
   - 下游长时间处理中场景下的主动补查与状态纠偏；
   - 审计、指标、告警与补偿任务联动。
4. P3: 退款业务域
   目标：在支付创建与支付结果闭环稳定后，复用既有安全、幂等、治理与路由骨架扩展退款能力。
   范围：
   - 退款创建；
   - 退款结果查询；
   - 退款回调/通知；
   - 退款异常与补偿语义。
5. P4: 通用交易查询与运维支撑能力
   目标：补齐上游与运营侧在交易生命周期中的辅助能力，但不提前侵入核心支付主链路。
   范围：
   - 统一交易查询/流水查询；
   - 面向运营的审计检索增强；
   - 商户、渠道、路由维度的业务可观测查询；
   - 更严格的业务级自动化验收与异常注入测试。

## Next Implementation Slice
- 当前技术脚手架计划已完成。
- `P0` 已完成第一个业务切片：本地 Mock Dubbo 下游编排。
- `P0` 已完成第二个业务切片：网关支付单与下游流水关联持久化。
- `P0` 已完成第三个业务切片：下游拒绝/失败分类、失败语义回放与失败日志路由补全。
- `P0` 已完成第四个业务切片：正式 DTO 映射、Dubbo DTO 序列化兼容修正，以及 `PROCESSING` 处理中的真实运行态闭环。
- `P0` 第五个业务切片已完成：真实 Docker 运行态下 `REQ-TIMEOUT* / REQ-ERROR*` 已分别返回 `504 DOWNSTREAM_TIMEOUT` 与 `502 DOWNSTREAM_SERVICE_ERROR`，失败回放、请求日志、异常事件、支付单/Outbox 边界都已完成验证。
- 当前 `P0` 的前五个业务切片和“真实下游服务契约替换首个切片”已经闭合，本地下游不再是纯前缀规则 Mock。
- `P1` 已完成：`/api/v1/payments/query` 与 `/api/v1/payments/callback` 已在真实 Docker 环境通过验签、状态持久化和 Dubbo 下游查询闭环验证。
- `P2` 第一个业务切片已完成：`/api/v1/payments/reconcile/processing`、定时调度补查、`PROCESSING -> SUCCEEDED` 状态纠偏、补偿失败异常事件与请求日志均已闭合。
- `P2` 第二个业务切片已完成：`/api/v1/messaging/outbox/retry` 已能重放失败 Outbox，成功记录会清空 `next_retry_time/last_error_message`，失败记录会保留失败语义并增加重试次数。
- `P2` 第三个业务切片已完成：通知消费侧已改为原生 `MessageExt`，运行态不再出现框架级 `convert failed`，消费成功记录已真实落库。
- `P3` 已完成：退款创建、查询、回调三条基线链路代码与测试已闭合，当前运行镜像内控制器可达。
- `P4` 已完成：统一交易查询与审计增强接口代码与测试已闭合，当前运行镜像内接口可达。
- 当前 `P0-P4` 在既定计划内已全部完成，且“更严格业务验收自动化 + 本地下游 stateful contract 替换”也已完成。
- 2026-04-24 进一步前移：
  - 完成了“本地下游 -> 真实外部 provider”的仓库内切换能力
  - 完成了商户安全配置的统一 provider 抽象、Nacos 导入链路和 refresh 边界
  - 完成了一组 `docs/delivery/` 交付级文档骨架
  - 完成了一套本地可靠性故障探针脚本，并拿到了 Redis / MySQL / RocketMQ Broker / Seata 的真实运行结论
- 下一步不再是补脚手架，也不再是继续堆本地 Mock，而是只剩真实交付口径事项：
  - 真实外部下游 provider 契约接入与联调
  - 商户密钥最终托管、轮换与灰度策略定板
  - 正式联调环境端到端证据沉淀
  - Redis/MySQL/RocketMQ/Seata 跨组件组合异常注入验收与最终语义确认

## Latest Closure (2026-04-23)
- Phase 24 is closed: stricter business-level acceptance automation and Docker build determinism.
- `/acceptance/index.html` now covers:
  - payment query closure / callback closure
  - refund query closure / refund callback closure
  - transaction detail / audit search / audit summary
  - outbox retry / notification retry / processing reconcile
  - reject / timeout / downstream error injection
- The runtime acceptance page drift is closed:
  - workspace source, `target/classes`, fat jar, container `app.jar`, and HTTP response are now aligned
  - runtime page contains `支付查询闭环`, `支付回调闭环`, `退款查询闭环`, `通知重试`, `支付超时注入`
- `gateway-app` image now builds from source inside Docker and still runs with exploded Spring Boot layout.
- Phase 25 is also closed: stateful local downstream contract replacement.
- Payment contract closure:
  - `MockPaymentCreateFacadeImpl` now persists provider-side state into `MockDownstreamPaymentStore`
  - `MockPaymentQueryFacadeImpl` resolves query status from the shared store before falling back to legacy prefix injection rules
  - targeted tests passed:
    - `MockPaymentCreateFacadeImplTest`
    - `MockPaymentQueryFacadeImplTest`
    - `DubboDownstreamPaymentCreateGatewayTest`
    - `DubboDownstreamPaymentQueryGatewayTest`
  - real Docker HTTP proof:
    - payment create returned `ACCEPTED`
    - subsequent payment query returned `SUCCEEDED`
    - processing payment create returned `PROCESSING`
    - subsequent payment query returned `PROCESSING`
- Refund contract closure:
  - `MockRefundFacadeImpl` now persists provider-side state into `MockDownstreamRefundStore`
  - refund query now resolves accepted refunds to `SUCCEEDED` from shared provider state
  - targeted test passed:
    - `MockRefundFacadeImplTest`
  - real Docker HTTP proof:
    - seed payment create returned `ACCEPTED`
    - refund create returned `ACCEPTED`
    - subsequent refund query returned `SUCCEEDED`
- Next focus:
  - replace local sandbox contracts with actual external downstream service contracts
  - extend abnormal-injection and combination-scenario verification around Redis/MySQL/RocketMQ/Seata
  - keep business scope stable until actual external-contract replacement is closed

## Developer Validation Page Plan
- Goal:
  - evolve current `/acceptance/index.html` from “acceptance page” into a “developer validation workbench”
  - keep validation page as the single入口 for day-to-day feature smoke checks, flow regression, and evidence collection
- Recommended implementation order:
  1. keep the current one-page foundation, do not create a second validation UI
  2. split page actions into four visible zones:
     - 环境状态区
     - 单功能验证区
     - 组合场景验证区
     - 证据与诊断区
  3. add editable scenario presets instead of fully hard-coded actions
  4. add operator-facing evidence export, so devs can copy request / response / latest IDs / links without opening code
- P0 page capabilities for developers:
  - one-click health checks for app / Nacos / Seata / SkyWalking / Prometheus
  - one-click payment / refund / query / callback / retry / reconcile actions
  - visible latest context for `gatewayPaymentId / gatewayRefundId / requestId / downstream ids`
  - PASS / FAIL / WARN result list
- P1 page capabilities for developers:
  - scenario presets:
    - success
    - duplicate
    - conflict
    - replay
    - bad signature
    - reject
    - timeout
    - downstream error
    - payment query closure
    - payment callback closure
    - refund query closure
    - refund callback closure
  - each preset should be both:
    - one-click runnable
    - manually editable before run
- P2 page capabilities for developers:
  - add “diagnostic shortcuts” after each run:
    - latest transaction detail query
    - latest audit query
    - latest outbox retry
    - latest notification retry
    - latest processing reconcile
  - add direct links to:
    - `/actuator/health`
    - `/actuator/prometheus`
    - Nacos console
    - SkyWalking UI
  - add copy buttons for:
    - latest curl command
    - latest request body
    - latest response body
    - latest requestId / gateway ids
- P3 page capabilities for developers:
  - add “suite mode”:
    - core suite
    - extended suite
    - reliability suite
  - each suite should show step-by-step progress and stop on hard failure
- Constraints:
  - do not expose destructive SQL or admin mutations directly on the page
  - keep the page within local/docker validation scope only
  - keep secrets fixed to local sample merchant material only

## Open Questions
- 商户密钥与接入配置下一步优先落在 Nacos、MySQL，还是采用配置/物料分离的混合模式。
- 当前 Dubbo 路由仍为技术占位；真实业务接入时如何定义服务发现、超时与错误分类边界。
- 是否在下一阶段优先补齐更严格的 Testcontainers 与异常注入测试，以覆盖 Redis/MySQL/RocketMQ/Seata 组合场景。
- P1 阶段的最终状态源以“异步回调”为主，还是需要引入“查询兜底 + 回调纠偏”的双轨模式。
- P3 阶段退款域是否复用支付主表扩展，还是拆分独立退款聚合与持久化模型。

## Developer Validation Workbench Closure (2026-04-23)
- The planned developer-facing validation scheme has now been implemented on the existing `/acceptance/index.html`.
- Closed capabilities:
  - environment and diagnostics area
  - preset-driven scenario editor
  - richer single-feature validation actions
  - core / extended / reliability suite progress state
  - latest evidence export for curl / request / response / context
- Closure criteria:
  - resource smoke test asserts the new workbench markers
  - documentation aligns with the new workbench workflow
  - Docker runtime must serve the same workbench markers as workspace source

## Errors Encountered
- 已解决：运行态与源码漂移。根因是旧容器未按最新代码与 Compose 配置重建。
- 已解决：`gateway-app` Docker 构建失败。根因是 Maven 未稳定产出 `gateway-app.jar`，已通过 `finalName + spring-boot repackage` 修复。
- 已解决：Nacos 3 启动失败。根因是容器脚本要求 `NACOS_AUTH_TOKEN` 与 `NACOS_AUTH_IDENTITY_*` 环境变量，且健康检查路径不再适配 `/nacos/actuator/health`。
- 已解决：Sentinel Dashboard 端口漂移。根因是容器实际监听 `8858`，而 Compose 和应用配置仍指向 `8080`。
- 已解决：`/acceptance/` 目录入口返回 500。根因是缺少到静态 `index.html` 的显式重定向。
- 已解决：本轮首次 `docker compose up -d --build gateway-app` 超时。根因是镜像重建耗时超过命令超时；镜像实际已完成构建，后续直接 `up -d gateway-app` 成功拉起容器。
- 已解决：PowerShell 7 下首次 HTTP 回归脚本读取错误响应体失败。根因是异常对象为 `HttpResponseMessage` 且响应内容已释放，已改为 `-SkipHttpErrorCheck` 直接读取状态与返回体。
- 已解决：支付创建成功路径返回 `500 INTERNAL_ERROR`。根因是 `gateway_idempotency_record.request_hash` 字段长度仅 `VARCHAR(128)`，无法容纳当前原始业务指纹；已将初始化脚本与运行态数据库列宽调整为 `VARCHAR(512)`。
- 已解决：下游失败请求虽然返回了正确错误码，但 `gateway_request_log.target_service` 为空。根因是失败路径记录请求日志时没有带入已命中的路由信息；现已按“仅后路由阶段失败才补路由信息”的方式修正。
- 已解决：运行态查库时误用了 `gateway_request_log.error_code` 与 `gateway_exception_event.severity` 旧列名。根因是查询脚本滞后于真实表结构；已改为当前实际字段 `error_type/error_message/event_level`。
- 已解决：`gateway-app` 最新镜像启动失败，报 `DubboDownstreamPaymentCreateGateway` 无默认构造器。根因是该类同时存在注入构造器和测试构造器，Spring 未能唯一判定；现已显式标注注入构造器。
- 已解决：本轮一次模块测试编译报 `PaymentCreateFacadeMapperImpl.java` 非法字符。根因是并行执行共享模块 Maven 任务导致 `generated-sources` 被竞争写入；已改为串行构建并清理生成目录后恢复。
- 已解决：本机 Docker Desktop/WSL 运行态一度失稳。恢复后已再次通过 `docker version`、`docker ps`、`docker compose ps gateway-app` 与 `/actuator/health` 验证，Phase 17 的真实 Docker 验收已闭合。
- 已解决：以 `sleep 3500ms` 模拟 provider 慢响应时，当前 `injvm` Dubbo 运行边界不会稳定转化为 consumer timeout，真实 HTTP 会误回 `200 ACCEPTED`。现已改为在 consumer 侧识别 `RpcException` 及其嵌套 cause 中的 timeout 文本语义，并恢复 provider 直接抛出超时异常，从而稳定映射到 `504 DOWNSTREAM_TIMEOUT`。
- 已解决：`P1` 查询链路首次进入 Docker 验收时，`gateway-app` 的 nested-jar 运行方式在 Linux 容器下反复触发 `GatewayException` 类加载异常，已改为 Spring Boot exploded 运行方式收敛。
- 已解决：`P1` 查询网关首次装配时，`DubboDownstreamPaymentQueryGateway` 缺少 `local/docker` profile 限定，且同时保留生产/测试构造器，导致出现“双 Bean”与“无默认构造器”两类运行态错误；现已分别通过 `@Profile` 与显式注入构造器修复。
- 已解决：`P2` 处理中订单补偿测试首次编译失败。根因是 `ProcessingPaymentReconcileApplicationServiceTest` 仍使用旧版 `GatewayRouteDefinition` 构造参数；现已按当前 9 字段契约修正。
- 已解决：`P2` 手工补偿首次看到 `updatedCount=0`。根因不是补偿未生效，而是已启用的定时任务先一步把应纠偏订单处理为 `SUCCEEDED`，手工接口拿到的是“剩余处理中集合”；现已通过数据库与请求日志证实调度器和手工接口都在真实运行。
- 已解决：`P2` Outbox 重试接口首次返回 `500`。根因是新控制器虽然已编译，但旧 `gateway-web` 产物未进入运行镜像；现已通过 `clean package + no-cache build + 删除旧容器重建` 收敛。
- 已解决：`P2` Outbox 重试成功后 `gateway_mq_outbox.next_retry_time / last_error_message` 未清空。根因是 MyBatis-Plus 对 `updateById` 和 `set(null)` 的默认行为未按预期清空字段；现已改为 `LambdaUpdateWrapper + setSql(... = NULL)`。
- 已解决：`P2` 通知消费侧在 Docker 运行态反复报 `convert failed / MessageConversionException`。根因是 `PaymentEventConsumer` 使用 `RocketMQListener<Message<String>>`，JSON 消息体在框架层被错误反序列化；现已改为消费原生 `MessageExt` 并手动提取 `keys/body`。
- 已解决：本轮 `gateway-app` 镜像首次重建时出现 `Invalid or corrupt jarfile /app/app.jar`。根因不是制品损坏，而是把 `mvn package` 与 `docker build` 并行执行，镜像在 jar 尚未写完时复制了半成品；现已改回串行打包后构建。
- 已解决：本轮 MQ 验收查库时误用了 `gateway_message_consume_record.dead_lettered` 与 `gateway_request_log.status` 旧列名；现已改为当前实际字段 `dead_letter / response_status`。
