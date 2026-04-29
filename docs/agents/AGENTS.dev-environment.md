### 如何快速定位模块

当你需要修改某类问题时，优先按职责定位模块，而不是全仓盲搜：

- 请求入口、Header 处理、参数绑定、统一响应、异常映射：
  查看 `gateway-web/`
- 验签、签名串构造、加解密、证书、时间戳、防重放：
  查看 `gateway-security/`
- 接口编排、下游路由、Dubbo 调用、事务控制、超时与容错处理：
  查看 `gateway-application/`
- 金额、币种、商户号、幂等号、请求时效校验等业务语义：
  查看 `gateway-domain/`
- Redis、MySQL、RocketMQ、Dubbo、Nacos、Seata、Redisson 客户端实现：
  查看 `gateway-infrastructure/`
- Sentinel 限流、降级、熔断、规则配置：
  查看 `gateway-governance/`
- 指标、trace、日志字段、审计埋点：
  查看 `gateway-observability/`

如果任务涉及 API 字段语义，先搜索：

- 请求 DTO / 响应 DTO
- Controller 入口
- Application Service 编排
- Dubbo 接口与下游 DTO
- Security 组件中的签名串生成逻辑
- 测试资源中的样例报文
- 指标名、trace tag、日志关键字段

### 本地依赖启动方式

本地开发推荐通过 Docker Compose 启动 MySQL、Redis 等依赖。若仓库已有 Compose 文件，优先使用仓库内文件；若无，可参考如下约定：

- 启动本地依赖：
  `docker compose -f docker/local-compose.yml up -d`

- 停止本地依赖：
  `docker compose -f docker/local-compose.yml down`

- 查看容器状态：
  `docker compose -f docker/local-compose.yml ps`

- 查看某个依赖日志：
  `docker compose -f docker/local-compose.yml logs -f mysql`
  `docker compose -f docker/local-compose.yml logs -f redis`
  `docker compose -f docker/local-compose.yml logs -f nacos`
  `docker compose -f docker/local-compose.yml logs -f rocketmq`

本地依赖通常包括：

- MySQL 8.4
- Redis 7.2.13
- RocketMQ
- Nacos
- Seata Server
- 可选的 Mock Dubbo 服务或测试桩服务

### 本地环境建议

- 使用 Java 17。
- 优先使用MapStruct、Lombok等成熟开发工具包，避免造轮子。
- 优先使用 `local` 或 `dev-local` Profile。
- 使用 Maven Wrapper，避免本机 Maven 版本偏差。
- 本地环境中的连接地址、用户名、密码、Nacos 命名空间、Dubbo 注册地址、Seata 事务组等，必须放在环境变量或未跟踪配置文件中。
- 严禁将本地测试地址、临时密码、调试 token 提交到仓库。
- 本地联调时优先使用 Mock Dubbo 服务或沙箱依赖，而不是连接共享测试环境。
- 修改 Spring MVC 相关逻辑时，要确认过滤器、拦截器、异常处理器、参数解析器顺序未被破坏。
- 修改 Sentinel 或 Nacos 配置读取逻辑时，要明确是“启动期静态读取”还是“运行期动态刷新”。
- 修改 Seata 或 Redisson 相关逻辑时，要先理解当前锁粒度、事务传播、分支提交/回滚模型。

### 调试建议

- 优先通过请求 ID、trace ID、商户号、幂等号定位请求链路；
- 安全失败优先排查：时间戳、签名串构造、编码方式、字段顺序、证书、字符集；
- Dubbo 调用失败优先排查：接口版本、超时、注册发现、序列化模型；
- Sentinel 问题优先排查：资源名定义、规则加载、热点参数、系统保护阈值；
- Nacos 配置问题优先排查：dataId、group、namespace、Profile 拼接规则、是否开启自动刷新；
- RocketMQ 问题优先排查：Topic/Tag、消费组、消息 Key、序列化格式；
- Redisson 问题优先排查：锁 key、锁超时、续期、锁重入与释放时机；
- Seata 问题优先排查：全局事务边界、分支事务注册、undo log、回滚原因。

### 临时排障产物约束

- 进行 `jar` 解包、`docker cp`、反编译、运行态字节码比对、日志探针或临时文件导出时，禁止把产物直接落在仓库根目录或正式源码目录下。
- 这类临时产物必须放在仓库内显式临时目录（如 `.tmp*/`）或系统临时目录，并在任务结束前清理。
- 特别禁止在仓库根目录残留以下结构或同类产物：
  - `BOOT-INF/`
  - `com/`
  - `lib/`
  - 解包后的 `.class`、`.jar`、运行态快照、临时日志
- 如果临时排障需要保留证据，优先把结论写入 `docs/` 或 `.agent/`，而不是保留解包目录本身。
