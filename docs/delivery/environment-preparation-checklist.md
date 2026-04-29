# 环境准备清单

## 1. 基础组件

- MySQL
- Redis
- Nacos
- RocketMQ NameServer
- RocketMQ Broker
- Seata Server
- SkyWalking OAP / UI

## 2. 账户与凭据

- MySQL 用户、密码、数据库名
- Redis 密码
- Nacos 地址、命名空间、Group、账号
- RocketMQ 地址、Topic、Tag、Consumer Group
- Seata 事务组、注册中心信息

注意：

- 真实密钥、证书、账户信息不得提交到仓库
- 正式环境凭据必须通过环境变量、Nacos、密钥系统或未跟踪配置注入

## 3. 网关侧关键环境变量

- `SPRING_PROFILES_ACTIVE`
- `GATEWAY_MYSQL_PORT`
- `GATEWAY_MYSQL_USERNAME`
- `GATEWAY_MYSQL_PASSWORD`
- `GATEWAY_REDIS_HOST`
- `GATEWAY_REDIS_PORT`
- `GATEWAY_REDIS_PASSWORD`
- `GATEWAY_NACOS_ADDR`
- `GATEWAY_NACOS_NAMESPACE`
- `GATEWAY_ROCKETMQ_NAMESRV`
- `GATEWAY_DUBBO_PORT`
- `GATEWAY_DOWNSTREAM_SANDBOX_ENABLED`
- `GATEWAY_SECURITY_CONFIG_SOURCE`

## 4. Nacos 数据项

当前仓库已使用或预留：

- `gateway-governance.json`
- `gateway-security.json`

建议：

- 治理类配置与商户安全配置分开存放
- 环境命名空间不要混用
- 正式验收环境先冻结 Data ID 和 Group

## 5. 验收前检查

- Docker 或宿主机端口未冲突
- 数据库 schema 已初始化
- Nacos Data ID 已导入
- Dubbo 注册中心可访问
- 真实回调地址已开通并可回流到网关
