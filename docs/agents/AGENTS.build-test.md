优先使用仓库自带的 Maven Wrapper；==如果仓库根目录未安装mvnw，则在项目根目录中使用`mvn -N io.takari:maven:wrapper -Dmaven=3.9.9`，然后配置Windows用户变量`MAVEN_USER_HOME`系统变量`setx MAVEN_USER_HOME "D:\programs\apache-maven-3.9.9\conf"`(复用本地Maven的配置)==。

### 构建命令

- 全量构建：
  `./mvnw clean package`

- 跳过测试进行快速构建：
  `./mvnw clean package -DskipTests`

- 构建指定模块并联动依赖模块：
  `./mvnw -pl gateway-web -am clean package`

- 仅编译不打包：
  `./mvnw clean compile`

### 测试命令

- 运行全部单元测试：
  `./mvnw test`

- 运行指定模块测试：
  `./mvnw -pl gateway-application test`

- 运行指定测试类：
  `./mvnw -Dtest=PaymentGatewayApplicationServiceTest test`

- 运行指定测试方法：
  `./mvnw -Dtest=PaymentGatewayApplicationServiceTest#should_route_request_to_internal_service test`

- 运行集成测试：
  `./mvnw verify -Pintegration-tests`

- 只运行某个集成测试类：
  `./mvnw -Pintegration-tests -Dit.test=GatewayRouteIntegrationTest verify`

### 格式化与静态检查命令

以下命令名称应以仓库实际插件配置为准；如果已经接入 Spotless、Checkstyle、PMD、SpotBugs，应优先使用现有约定。

- 代码格式检查：
  `./mvnw spotless:check`

- 自动格式化：
  `./mvnw spotless:apply`

- Checkstyle 检查：
  `./mvnw checkstyle:check`

- PMD 检查：
  `./mvnw pmd:check`

- SpotBugs 检查：
  `./mvnw spotbugs:check`

- 全量校验：
  `./mvnw clean verify`

### 本地启动命令

- 使用本地环境配置启动：
  `./mvnw -pl gateway-app spring-boot:run -Dspring-boot.run.profiles=local`

- 打包后运行：
  `java -jar gateway-app/target/gateway-app.jar --spring.profiles.active=local`

- 指定 Nacos、Redis、MySQL 等参数时，优先通过环境变量、`application-local.yml` 或未跟踪的本地覆盖文件提供，不要直接改提交配置。

### 代理执行策略

代理在验证修改时，必须遵守以下顺序：

1. 先运行与改动最相关的最小测试集；
2. 如果改动跨多个模块，补充运行模块级测试；
3. 如果改动涉及安全、协议、限流熔断、Dubbo 接口、消息模型、事务、分布式锁或公共组件，必须做更大范围验证；
4. 非必要不要一开始就跑全仓测试；
5. 最终说明中必须明确写出“实际执行了哪些命令”。