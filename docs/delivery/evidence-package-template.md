# 交付级验收证据模板

建议按一次正式验收生成一个证据包目录，例如：

```text
evidence/
  2026-04-24-user-uat/
    01-environment.txt
    02-version.txt
    03-payment-success/
    04-payment-failure/
    05-refund-success/
    06-transaction-query/
    07-mq-retry/
    08-observability/
    09-summary.md
```

## 1. 必备内容

- 版本号、镜像 tag、提交 SHA
- 环境说明
- 关键配置来源说明
- 请求样例与响应样例
- 数据库状态变化证据
- MQ 消费 / 重试证据
- 观测平台截图或操作步骤
- 最终验收结论

## 2. 推荐命名

- `request.json`
- `response.json`
- `curl.txt`
- `db-check.txt`
- `mq-check.txt`
- `trace.txt`
- `summary.md`

## 3. 最终结论模板

- 验收范围：
- 验收环境：
- 版本信息：
- 通过项：
- 未通过项：
- 风险与后续动作：
