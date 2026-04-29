本仓库按“接口接入、应用编排、领域规则、基础设施、安全能力、流量治理、可观测性、配置与测试资源”分层组织。代理应优先在既有分层内修改，不要随意跨层堆积逻辑。

典型目录结构如下：

- `pom.xml`
  根构建文件，统一管理依赖版本、插件、模块聚合。
- `gateway-app/`
  应用启动模块，包含主启动类、自动装配入口、运行时装配配置。
- `gateway-api/`
  对外 API 契约、请求/响应模型、公开常量、错误码定义。
- `gateway-web/`
  Spring MVC 控制器、参数绑定、拦截器、过滤器、统一响应、全局异常处理。
- `gateway-application/`
  应用服务层，负责编排流程、调用安全组件、协议转换、下游路由、事务边界与容错治理。
- `gateway-domain/`
  核心领域对象、值对象、校验规则、幂等与重放保护规则、业务语义模型。
- `gateway-infrastructure/`
  MySQL、Redis、RocketMQ、Nacos、Dubbo、Seata、Redisson 适配、DAO、客户端与持久化实现。
- `gateway-security/`
  加签验签、加解密、证书装载、密钥引用、脱敏、重放保护、安全工具类。
- `gateway-governance/`
  Sentinel 规则接入、限流熔断、隔离降级、热点防护、系统保护、调用策略。
- `gateway-observability/`
  Micrometer 指标、SkyWalking 埋点、日志字段约定、审计事件封装。
- `gateway-common/`
  通用枚举、基础工具、统一常量、响应模型、异常基类、上下文传播工具。
- `src/main/resources/` 或各模块 `src/main/resources/`
  配置文件、Nacos 配置模板、日志配置、限流规则模板、消息模板、映射配置。
- `src/test/java/`
  测试代码，原则上镜像生产代码包结构。
- `src/test/resources/`
  测试资源、样例报文、Mock 配置、测试证书、容器初始化脚本。
- `docker/`
  本地开发依赖、Dockerfile、Compose 编排文件。
- `scripts/`
  启停脚本、开发辅助脚本、CI 辅助脚本。
- `docs/`
  架构设计、接口协议、验签规则、联调说明、排障手册、流量治理说明。
- `sql/` 或 `db/`
  DDL、初始化脚本、迁移脚本、测试数据脚本。
- `.agent/`
  代理工作文件目录，统一存放 `planning-with-files` 等 agent 协作产物，例如 `task_plan.md`、`findings.md`、`progress.md`。

### Agent Working Files

- 所有 agent 侧规划、过程记录、发现结论，统一放在项目根目录 `.agent/` 下。
- `.agent/` 目录属于协作工作区，不属于业务实现层，不要把业务代码、运行配置或正式交付物混放进去。
- 如果任务需要持久化工作上下文，优先复用 `.agent/task_plan.md`、`.agent/findings.md`、`.agent/progress.md`，不要再在项目根目录散落同类文件。

### 关键业务约束

以下目录和逻辑属于高风险区域，除非任务明确要求，否则不要进行结构性重写：

- `gateway-security/`
  涉及签名、验签、加解密、证书、密钥引用、重放保护。此类逻辑只能做最小必要修改。
- `gateway-governance/`
  涉及限流、熔断、隔离、降级。错误修改可能直接影响流量入口稳定性。
- `gateway-web/` 中的过滤器、拦截器、全局异常处理器、请求上下文组件
  此处可能承载请求 ID、时间戳校验、鉴权、签名、日志、租户信息传递等基础能力。
- `gateway-application/` 中的协议转换、路由编排、事务边界
  不得随意改变字段语义、默认值、调用顺序、重试策略、事务传播属性。
- `gateway-domain/` 中的幂等、时钟、签名串构造、金额与币种校验规则
  不得随意“简化”或重命名导致行为变化。
- `gateway-infrastructure/` 中的 Seata、Redisson、RocketMQ、Dubbo 适配层
  不得擅自调整资源名、锁 key、事务分组、topic、tag、消费者组、超时等语义。
- 配置中心相关代码
  不得将动态配置固化到本地代码，也不得绕过统一配置读取路径。
- 指标、追踪、审计日志相关代码
  不得随意删除、改名、合并指标标签或 trace 传递逻辑。

代理在修改前应先判断需求属于哪一层：

- 若是请求协议、鉴权、安全校验，优先看 `gateway-web/` 与 `gateway-security/`
- 若是路由、编排、事务、容错、聚合，优先看 `gateway-application/`
- 若是规则、值对象、校验语义、幂等，优先看 `gateway-domain/`
- 若是 DB、Redis、MQ、Dubbo、Nacos、Seata、Redisson 接入，优先看 `gateway-infrastructure/`
- 若是限流、熔断、隔离，优先看 `gateway-governance/`
- 若是指标、日志、追踪、审计，优先看 `gateway-observability/`