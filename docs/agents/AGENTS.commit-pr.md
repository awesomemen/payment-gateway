### 提交格式

提交信息应简洁、明确、可追踪，推荐使用如下格式：

- `feat(gateway-web): 新增商户请求时间戳校验`
- `fix(gateway-security): 修复签名串字段顺序错误`
- `refactor(gateway-application): 收敛路由编排中的重复异常转换`
- `test(gateway-domain): 补充金额精度回归测试`
- `chore(build): 调整 Maven 插件版本`

推荐类型包括：

- `feat`
- `fix`
- `refactor`
- `test`
- `chore`
- `docs`

作用域建议使用模块名，如：

- `gateway-web`
- `gateway-security`
- `gateway-application`
- `gateway-domain`
- `gateway-infrastructure`
- `gateway-governance`
- `gateway-observability`

### 提交要求

- 每次提交聚焦单一目的；
- 不要把格式化、重构、功能变更、测试补丁混在一次提交中；
- 高风险逻辑变更应拆成更小、可审查的提交；
- 不要提交调试代码、临时日志、注释掉的大块旧代码；
- 不要声称“已测试”除非实际执行过相关命令。

### PR Checklist

PR 描述至少应包含以下内容：

- 本次改动解决了什么问题；
- 改动位于哪些模块；
- 是否涉及以下高风险内容：
  - 安全校验
  - 签名 / 验签
  - 加解密
  - 协议字段变化
  - Dubbo 接口契约
  - Sentinel 规则
  - Nacos 配置读取
  - Redis / MySQL / RocketMQ 行为
  - Seata 事务边界
  - Redisson 分布式锁
  - 指标 / trace / 日志字段
- 是否存在配置变更；
- 是否存在数据结构或消息模型变更；
- 新增或修改了哪些测试；
- 实际运行了哪些验证命令；
- 当前仍未覆盖的风险或限制。

建议在 PR 中使用如下检查项：

- [ ] 已确认改动范围最小化
- [ ] 已评估接口兼容性
- [ ] 已评估安全影响
- [ ] 已评估流量治理影响
- [ ] 已评估事务与锁影响
- [ ] 已评估可观测性影响
- [ ] 已补充或更新测试
- [ ] 已列出实际执行的命令
- [ ] 未包含真实密钥、地址、凭证或敏感数据