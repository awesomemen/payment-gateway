# 本地可靠性验收脚本

当前仓库已新增一个可复跑的本地可靠性验收脚本：

- `scripts/local-reliability-suite.ps1`

它的目标不是替代真实用户联调，而是把当前本地 Docker 环境里可重复的组件故障探针固化下来，减少手工 stop/start 服务、手工签名请求和手工查库的成本。

## 覆盖场景

当前脚本覆盖：

1. `Redis` 掉线时的支付创建探针
2. `MySQL` 掉线时的支付创建探针
3. `RocketMQ Broker` 掉线时的支付创建 + Outbox 失败记录 + 服务恢复后重试探针
4. `Seata` 掉线时的支付创建探针

注意：

- 这套脚本目前是“本地组件故障探针”，不是最终正式交付环境的完整故障演练
- `Seata` 场景当前会把观测结果标为 `REVIEW`，因为它涉及最终事务策略，不应在没有架构结论时被脚本硬编码为绝对通过/失败

## 使用方式

在仓库根目录、当前 PowerShell 会话内执行：

```powershell
& .\scripts\local-reliability-suite.ps1
```

可选参数：

```powershell
& .\scripts\local-reliability-suite.ps1 `
  -BaseUrl http://localhost:18081 `
  -OutputDir .\.tmp-reliability\manual-run
```

## 输出位置

默认输出到：

- `.tmp-reliability/<timestamp>/summary.md`
- `.tmp-reliability/<timestamp>/summary.json`
- 各场景单独的 `*.json`

这样做的目的是：

- 不把运行态证据直接混进正式源码目录
- 仍然保留一份可复核的本地证据目录

已验证方式：

- 当前仓库 PowerShell 会话内直接执行 `& .\scripts\local-reliability-suite.ps1`

当前已验证的一次成功证据目录：

- `.tmp-reliability/20260424-113208`

## 当前脚本内置判断

- `Redis` / `MySQL` 场景：
  - 重点看系统是否快速失败，而不是静默成功
- `RocketMQ Broker` 场景：
  - 重点看主单是否仍可受理
  - 同时看 Outbox 是否进入失败态
  - 服务恢复后是否能完成补偿重试
- `Seata` 场景：
  - 重点看当前系统的真实表现和恢复情况
  - 最终是否接受这种行为，仍需要结合架构决策确认
