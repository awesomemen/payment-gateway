# 发布与回滚说明

## 1. 发布前

- 确认目标版本号、镜像 tag、提交 SHA
- 确认验收环境配置已准备完成
- 确认 `gateway-security.json`、`gateway-governance.json` 已导入
- 确认 `GATEWAY_DOWNSTREAM_SANDBOX_ENABLED` 已按目标环境设置

## 2. 推荐发布步骤

1. 构建应用镜像
2. 部署 `gateway-app`
3. 检查 `docker compose ps` 或等价运行态状态
4. 检查 `GET /actuator/health`
5. 执行最小 smoke：
   - 支付创建
   - 支付查询
   - 交易详情查询

## 3. 发布后核对

- 健康检查为 `UP`
- Nacos 注册正常
- Dubbo consumer / provider 可发现
- MQ 配置已生效
- 关键审计日志可写入

## 4. 回滚条件

出现以下情况建议回滚：

- 应用无法启动
- 健康检查持续失败
- 支付创建主链路不可用
- 回调或查询链路严重异常
- 事务 / MQ 异常导致状态不一致

## 5. 回滚步骤

1. 切回上一版镜像或包
2. 恢复上一版 Nacos 配置
3. 重启应用实例
4. 再次检查健康检查和最小 smoke
5. 记录本次回滚原因和证据
